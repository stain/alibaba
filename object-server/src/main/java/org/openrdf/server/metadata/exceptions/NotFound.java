package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The server has not found anything matching the request-target. No indication
 * is given of whether the condition is temporary or permanent. The 410 (Gone)
 * status code SHOULD be used if the server knows, through some internally
 * configurable mechanism, that an old resource is permanently unavailable and
 * has no forwarding address. This status code is commonly used when the server
 * does not wish to reveal exactly why the request has been refused, or when no
 * other response is applicable.
 */
public class NotFound extends ResponseException {
	private static final long serialVersionUID = -2946832304266899273L;

	public NotFound() {
		super("Not Found");
	}

	public NotFound(String message) {
		super(message);
	}

	public NotFound(String message, Throwable cause) {
		super(message, cause);
	}

	public NotFound(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 404;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
