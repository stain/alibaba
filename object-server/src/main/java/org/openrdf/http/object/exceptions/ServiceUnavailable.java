package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

public class ServiceUnavailable extends ResponseException {
	private static final long serialVersionUID = -3465974175019250404L;

	public ServiceUnavailable() {
		super("Service Unavailable");
	}

	public ServiceUnavailable(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceUnavailable(String message) {
		super(message);
	}

	public ServiceUnavailable(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 503;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
