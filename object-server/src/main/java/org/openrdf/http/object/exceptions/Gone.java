package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

/**
 * The requested resource is no longer available at the server and no forwarding
 * address is known. This condition is expected to be considered permanent.
 * Clients with link editing capabilities SHOULD delete references to the
 * request-target after user approval. If the server does not know, or has no
 * facility to determine, whether or not the condition is permanent, the status
 * code 404 (Not Found) SHOULD be used instead. This response is cacheable
 * unless indicated otherwise.
 */
public class Gone extends ResponseException {
	private static final long serialVersionUID = 3422241245426476225L;

	public Gone() {
		super("Gone");
	}

	public Gone(String message) {
		super(message);
	}

	public Gone(String message, Throwable cause) {
		super(message, cause);
	}

	public Gone(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 410;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
