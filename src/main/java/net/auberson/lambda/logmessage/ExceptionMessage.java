package net.auberson.lambda.logmessage;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Strings;

public class ExceptionMessage implements Message {
	private static final long serialVersionUID = 1L;

	private final Exception e;
	
	public ExceptionMessage(Exception e) {
		this.e = e;
	}

	@Override
	public String getFormattedMessage() {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString().replace('\n', '\r');
	}

	@Override
	public String getFormat() {
		return Strings.EMPTY;
	}

	@Override
	public Object[] getParameters() {
		return null;
	}

	@Override
	public Throwable getThrowable() {
		return null;
	}

}
