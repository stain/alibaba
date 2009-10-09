package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The server, while acting as a gateway or proxy, received an invalid response
 * from the upstream server it accessed in attempting to fulfill the request.
 */
public class BadGateway extends ResponseException {
	private static final long serialVersionUID = -36400574693503548L;

	public BadGateway() {
		super("Bad Gateway");
	}

	public BadGateway(String message, Throwable cause) {
		super(message, cause);
	}

	public BadGateway(String message) {
		super(message);
	}

	public BadGateway(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 502;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
