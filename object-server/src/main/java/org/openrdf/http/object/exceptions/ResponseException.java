/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.exceptions;

import java.io.PrintWriter;

/**
 * Base class for HTTP exceptions.
 */
public abstract class ResponseException extends RuntimeException {

	public static ResponseException create(final int status, String msg, String stack) {
		switch (status) {
		case 502:
			return new BadGateway(msg, stack);
		case 400:
			return new BadRequest(msg, stack);
		case 409:
			return new Conflict(msg, stack);
		case 504:
			return new GatewayTimeout(msg, stack);
		case 410:
			return new Gone(msg, stack);
		case 415:
			return new UnsupportedMediaType(msg, stack);
		case 500:
			return new InternalServerError(msg, stack);
		case 405:
			return new MethodNotAllowed(msg, stack);
		case 406:
			return new NotAcceptable(msg, stack);
		case 404:
			return new NotFound(msg, stack);
		case 501:
			return new NotImplemented(msg, stack);
		case 503:
			return new ServiceUnavailable(msg, stack);
		default:
			return new ResponseException(msg, stack) {
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
	private String msg;

	public ResponseException(String message) {
		super(message);
		this.msg = trimMessage(message);
	}

	public ResponseException(String message, Throwable cause) {
		super(message, cause);
		this.msg = trimMessage(message);
	}

	public ResponseException(Throwable cause) {
		super(cause);
		this.msg = trimMessage(cause.toString());
	}

	public ResponseException(String message, String stack) {
		super(stack);
		this.msg = message;
	}

	public abstract int getStatusCode();

	public abstract void printTo(PrintWriter writer);

	@Override
	public String getMessage() {
		return msg;
	}

	public String getDetailMessage() {
		return super.getMessage();
	}

	private String trimMessage(String msg) {
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
