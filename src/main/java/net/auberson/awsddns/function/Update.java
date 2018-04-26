package net.auberson.awsddns.function;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.GetHostedZoneResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

import net.auberson.awsddns.model.ServerlessInput;
import net.auberson.awsddns.model.ServerlessOutput;
import net.auberson.lambda.logmessage.ExceptionMessage;
import net.auberson.lambda.logmessage.MapMessage;

/**
 * Lambda function that is triggered by the API Gateway when a DDNS client sends
 * an Update
 */
public class Update implements RequestHandler<ServerlessInput, ServerlessOutput> {
	static final Logger log = LogManager.getLogger(Update.class);

	// Identifier of the Hosted Zone
	private static final String HOSTED_ZONE_ID = System.getenv("HOSTED_ZONE_ID");

	// Parameters passed in the query string
	private static final String PARAM_HOSTNAME = "hostname";
	private static final String PARAM_IP = "myip";

	@Override
	public ServerlessOutput handleRequest(ServerlessInput serverlessInput, Context context) {

		// Using builder to create the clients could allow us to dynamically load the
		// region from the AWS_REGION environment variable. This way, we can deploy the
		// Lambda functions to different regions without code change.
		AmazonRoute53 route53 = AmazonRoute53ClientBuilder.standard().build();
		ServerlessOutput output = new ServerlessOutput();

		log.info(new MapMessage("Query Parameters:", serverlessInput.getQueryStringParameters()));
		log.info(new MapMessage("Request Headers:", serverlessInput.getHeaders()));
		
		try {
			if (serverlessInput.getQueryStringParameters() == null) {
				throw new Exception("No parameters. Query string parameter must be provided: " + PARAM_HOSTNAME);
			}

			String hostname = serverlessInput.getQueryStringParameters().get(PARAM_HOSTNAME);
			if (hostname == null) {
				throw new Exception("Query string parameter must be provided: " + PARAM_HOSTNAME);
			}

			String myip = serverlessInput.getQueryStringParameters().get(PARAM_IP);
			if (myip == null) {
				// If no IP address was passed, retrieve the original sender's IP address from
				// the headers (courtesy of the API gateway)
				myip = serverlessInput.getHeaders().get("X-Forwarded-For");
			}

			GetHostedZoneRequest getHostedZoneRequest = new GetHostedZoneRequest(HOSTED_ZONE_ID);
			GetHostedZoneResult hostedZone = route53.getHostedZone(getHostedZoneRequest);
			// The domain name is the zone name, minus the trailing dot
			final String domainName = hostedZone.getHostedZone().getName().replaceAll("\\.$", "");
			if (!hostname.endsWith(domainName)) {
				throw new Exception("Hostname " + hostname + " is not part of domain " + domainName);
			}

			ListResourceRecordSetsRequest listResourceRecordSetsRequest = new ListResourceRecordSetsRequest(
					HOSTED_ZONE_ID);
			listResourceRecordSetsRequest.setStartRecordName(hostname);
			listResourceRecordSetsRequest.setStartRecordType(RRType.A);
			listResourceRecordSetsRequest.setMaxItems("1");
			ListResourceRecordSetsResult recordSets = route53.listResourceRecordSets(listResourceRecordSetsRequest);
			String oldip = null;
			for (ResourceRecordSet recordSet : recordSets.getResourceRecordSets()) {
				for (ResourceRecord record : recordSet.getResourceRecords()) {
					if (oldip != null) {
						throw new Exception("Multiple IP are not supported for " + domainName + ": " + oldip + ", "
								+ record.getValue());
					}
					oldip = record.getValue().trim();
				}
			}

			log.info("Old IP is '{}', new IP is '{}'", oldip, myip);

			if (myip.trim().equals(oldip)) {
				output.setBody("nochg " + myip);
				output.setStatusCode(200);
				return output;
			}

			List<ResourceRecord> resourceRecords = new ArrayList<ResourceRecord>();
			resourceRecords.add(new ResourceRecord(myip));
			ResourceRecordSet recordSet = new ResourceRecordSet(hostname, RRType.A);
			recordSet.setTTL(300l);
			recordSet.setResourceRecords(resourceRecords);
			List<Change> changes = new ArrayList<Change>();
			if (oldip == null) {
				changes.add(new Change(ChangeAction.CREATE, recordSet));
			} else {
				changes.add(new Change(ChangeAction.UPSERT, recordSet));
			}
			ChangeBatch changeBatch = new ChangeBatch(changes);
			ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest = new ChangeResourceRecordSetsRequest(
					HOSTED_ZONE_ID, changeBatch);
			log.info("Route53 Request: " + changeResourceRecordSetsRequest.toString());
			route53.changeResourceRecordSets(changeResourceRecordSetsRequest);
			output.setBody("good " + myip);
			output.setStatusCode(200);
			return output;
		} catch (Exception e) {
			output.setStatusCode(500);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log.error(new ExceptionMessage(e));
			output.setBody("badagent (" + sw.toString() + ")");
			return output;
		}
	}

}