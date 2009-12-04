package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

public abstract class ResponseException extends RuntimeException {

	public static ResponseException create(final int status, String msg, String stack) {
		if (stack != null && stack.length() > 0) {
			msg = stack;
		}
		switch (status) {
		case 502:
			return new BadGateway(msg);
		case 400:
			return new BadRequest(msg);
		case 409:
			return new Conflict(msg);
		case 504:
			return new GatewayTimeout(msg);
		case 410:
			return new Gone(msg);
		case 500:
			return new InternalServerError(msg);
		case 405:
			return new MethodNotAllowed(msg);
		case 404:
			return new NotFound(msg);
		case 501:
			return new NotImplemented(msg);
		case 503:
			return new ServiceUnavailable(msg);
		default:
			return new ResponseException(msg) {
				private static final long serialVersionUID = 3458241161561417132L;

				public void printTo(PrintWriter writer) {
					writer.write(getMessage());
				}

				public int getStatusCode() {
					return status;
				}
			};
		}
	}

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

	public String getDetailMessage() {
		return super.getMessage();
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
