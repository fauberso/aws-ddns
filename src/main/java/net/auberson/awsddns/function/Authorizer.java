package net.auberson.awsddns.function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.Base64;

import net.auberson.awsddns.model.AuthPolicy;
import net.auberson.awsddns.model.ServerlessInput;
import net.auberson.awsddns.model.ServerlessInput.RequestContext;
import net.auberson.lambda.logmessage.MapMessage;

/**
 * Lambda function that is triggered by the API Gateway when a DDNS client sends
 * an Update
 */
public class Authorizer implements RequestHandler<ServerlessInput, AuthPolicy> {
	static final Logger log = LogManager.getLogger(Authorizer.class);

	// Identifier of the Hosted Zone
	private static final String USERNAME = System.getenv("USERNAME");
	private static final String PASSWORD = System.getenv("PASSWORD");
	
	// See https://docs.aws.amazon.com/lambda/latest/dg/current-supported-versions.html#lambda-environment-variables
	private static final String REGION = System.getenv("AWS_REGION");

	@Override
	public AuthPolicy handleRequest(ServerlessInput serverlessInput, Context context) {

		AuthPolicy output = new AuthPolicy();

		//log.info(new MapMessage("Query Parameters:", serverlessInput.getQueryStringParameters()));
		//log.info(new MapMessage("Request Headers:", serverlessInput.getHeaders()));
		//log.info(new MapMessage("Path Parameters:", serverlessInput.getPathParameters()));
		//log.info(new MapMessage("Stage Variables:", serverlessInput.getStageVariables()));
		//log.info("Body:", serverlessInput.getBody());

		final RequestContext ctx = serverlessInput.getRequestContext();
		String authorization = serverlessInput.getHeaders().get("Authorization");
		final String[] credentials;
		
		if (authorization==null) {
			log.info("No Authorization header, denying access");
			output.setPolicyDocument(AuthPolicy.PolicyDocument.getDenyAllPolicy(REGION, ctx.getAccountId(), ctx.getApiId(), ctx.getStage()));
			return output;
		}
		
		if (!authorization.contains(":")) {
			authorization = Base64.decode(authorization).toString();
		}
		
		credentials=authorization.split(":");
		
		if (credentials.length<2) {
			log.error("Missing Password in Credentials: "+authorization);
			output.setPolicyDocument(AuthPolicy.PolicyDocument.getDenyAllPolicy(REGION, ctx.getAccountId(), ctx.getApiId(), ctx.getStage()));
			return output;
		} else if (credentials.length>2) {
			log.info("Extraneous elements in Credentials: "+authorization);
		}
		
		if (!USERNAME.equals(credentials[0])) {
			log.error("Unknown User: "+credentials[0]);
			output.setPolicyDocument(AuthPolicy.PolicyDocument.getDenyAllPolicy(REGION, ctx.getAccountId(), ctx.getApiId(), ctx.getStage()));
			return output;
		}
		
		
		if (!PASSWORD.equals(credentials[1])) {
			log.error("Password mismatch");
			output.setPolicyDocument(AuthPolicy.PolicyDocument.getDenyAllPolicy(REGION, ctx.getAccountId(), ctx.getApiId(), ctx.getStage()));
			return output;
		}
		
		log.info("Access granted by Lambda Authorizer");
		output.setPolicyDocument(AuthPolicy.PolicyDocument.getAllowAllPolicy(REGION, ctx.getAccountId(), ctx.getApiId(), ctx.getStage()));
		return output;
	}
}