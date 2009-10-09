package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The server does not support the functionality required to fulfill the
 * request. This is the appropriate response when the server does not recognize
 * the request method and is not capable of supporting it for any resource.
 */
public class NotImplemented extends ResponseException {
	private static final long serialVersionUID = 4773581231132742758L;

	public NotImplemented() {
		super("Not Implemented");
	}

	public NotImplemented(String message, Throwable cause) {
		super(message, cause);
	}

	public NotImplemented(String message) {
		super(message);
	}

	public NotImplemented(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 501;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
