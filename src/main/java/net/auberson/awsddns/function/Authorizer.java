package net.auberson.awsddns.function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import net.auberson.awsddns.model.AuthPolicy;
import net.auberson.awsddns.model.ServerlessInput;
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

	@Override
	public AuthPolicy handleRequest(ServerlessInput serverlessInput, Context context) {

		AuthPolicy output = new AuthPolicy();

		log.info(new MapMessage("Query Parameters:", serverlessInput.getQueryStringParameters()));
		log.info(new MapMessage("Request Headers:", serverlessInput.getHeaders()));
		log.info(new MapMessage("Path Parameters:", serverlessInput.getPathParameters()));
		log.info(new MapMessage("Stage Variables:", serverlessInput.getStageVariables()));
		log.info("Body:", serverlessInput.getBody());

		output.setPolicyDocument(new AuthPolicy.PolicyDocument("", "", "", ""));
		return output;
	}
}