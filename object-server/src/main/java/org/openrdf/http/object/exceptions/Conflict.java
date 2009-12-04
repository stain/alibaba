package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

/**
 * The request could not be completed due to a conflict with the current state
 * of the resource. This code is only allowed in situations where it is expected
 * that the user might be able to resolve the conflict and resubmit the request.
 * The response body SHOULD include enough information for the user to recognize
 * the source of the conflict. Ideally, the response entity would include enough
 * information for the user or user agent to fix the problem; however, that
 * might not be possible and is not required.
 */
public class Conflict extends ResponseException {
	private static final long serialVersionUID = -3593948846440460801L;

	public Conflict() {
		super("Conflict");
	}

	public Conflict(String message) {
		super(message);
	}

	public Conflict(String message, Throwable cause) {
		super(message, cause);
	}

	public Conflict(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 409;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
