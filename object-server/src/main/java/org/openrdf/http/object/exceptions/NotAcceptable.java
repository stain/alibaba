package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

/**
 * The resource identified by the request is only capable of generating response
 * entities which have content characteristics not acceptable according to the
 * accept headers sent in the request.
 */
public class NotAcceptable extends ResponseException {
	private static final long serialVersionUID = 630976310525141097L;

	public NotAcceptable() {
		super("Not Acceptable");
	}

	public NotAcceptable(String message) {
		super(message);
	}

	public NotAcceptable(String message, Throwable cause) {
		super(message, cause);
	}

	public NotAcceptable(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 406;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
