package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The method specified in the Request-Line is not allowed for the resource
 * identified by the request-target. The response MUST include an Allow header
 * containing a list of valid methods for the requested resource.
 */
public class MethodNotAllowed extends ResponseException {
	private static final long serialVersionUID = -3469466804938359945L;

	public MethodNotAllowed() {
		super("Method Not Allowed");
	}

	@Override
	public int getStatusCode() {
		return 405;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
