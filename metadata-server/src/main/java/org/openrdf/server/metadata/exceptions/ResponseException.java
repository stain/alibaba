package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

public abstract class ResponseException extends RuntimeException {
	private static final long serialVersionUID = -4156041448577237448L;

	public ResponseException(String message) {
		super(message);
	}

	public ResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResponseException(Throwable cause) {
		super(cause);
	}

	public abstract int getStatusCode();

	public abstract void printTo(PrintWriter writer);

	@Override
	public String getMessage() {
		String msg = super.getMessage();
		if (msg == null) {
			Throwable cause = getCause();
			if (cause == null) {
				msg = getClass().getName();
			} else {
				msg = cause.getClass().getName();
			}
		}
		if (msg.contains("\r")) {
			msg = msg.substring(0, msg.indexOf('\r'));
		}
		if (msg.contains("\n")) {
			msg = msg.substring(0, msg.indexOf('\n'));
		}
		return trimExceptionClass(msg, this);
	}

	private String trimExceptionClass(String msg, Throwable cause) {
		if (cause == null)
			return msg;
		String prefix = cause.getClass().getName() + ": ";
		if (msg.startsWith(prefix)) {
			msg = msg.substring(prefix.length());
		}
		return trimExceptionClass(msg, cause.getCause());
	}

}
