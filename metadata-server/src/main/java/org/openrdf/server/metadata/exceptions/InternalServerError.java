package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The server encountered an unexpected condition which prevented it from
 * fulfilling the request.
 */
public class InternalServerError extends ResponseException {
	private static final long serialVersionUID = 6899453578370539031L;

	public InternalServerError() {
		super("Internal Server Error");
	}

	public InternalServerError(String message, Throwable cause) {
		super(message, cause);
	}

	public InternalServerError(String message) {
		super(message);
	}

	public InternalServerError(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 500;
	}

	@Override
	public void printTo(PrintWriter writer) {
		Throwable cause = getCause();
		if (cause == null) {
			writer.write(getMessage());
		} else {
			cause.printStackTrace(writer);
		}
	}

}
