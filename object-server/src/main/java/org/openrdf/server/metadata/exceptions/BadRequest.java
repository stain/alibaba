package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The request could not be understood by the server due to malformed syntax.
 * The client SHOULD NOT repeat the request without modifications.
 */
public class BadRequest extends ResponseException {
	private static final long serialVersionUID = -35616916296397667L;

	public BadRequest() {
		super("Bad Request");
	}

	public BadRequest(String message) {
		super(message);
	}

	public BadRequest(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequest(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 400;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
