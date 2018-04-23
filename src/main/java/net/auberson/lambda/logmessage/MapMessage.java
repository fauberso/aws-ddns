package net.auberson.lambda.logmessage;

import java.util.Map;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Strings;

public class MapMessage implements Message {
	private static final long serialVersionUID = 1L;
	
	private final String heading;
	private final Map<String, String> entries;

	public MapMessage(String heading, Map<String, String> entries) {
		super();
		this.heading = heading;
		this.entries = entries;
	}

	@Override
	public String getFormat() {
		return Strings.EMPTY;
	}

	@Override
	public String getFormattedMessage() {
		final StringBuilder sb = new StringBuilder(heading).append('\r');
		for (Map.Entry<String, String> entry : entries.entrySet()) {
			sb.append(" - ").append(entry.getKey());
			sb.append(": ").append(entry.getValue());
			sb.append('\r');
		}
		return sb.toString();
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
