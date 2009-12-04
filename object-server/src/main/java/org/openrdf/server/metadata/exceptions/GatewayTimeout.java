package org.openrdf.server.metadata.exceptions;

import java.io.PrintWriter;

/**
 * The server, while acting as a gateway or proxy, did not receive a timely
 * response from the upstream server specified by the URI (e.g. HTTP, FTP, LDAP)
 * or some other auxiliary server (e.g. DNS) it needed to access in attempting
 * to complete the request.
 */
public class GatewayTimeout extends ResponseException {
	private static final long serialVersionUID = -7878209025109522123L;

	public GatewayTimeout() {
		super("Gateway Timeout");
	}

	public GatewayTimeout(String message, Throwable cause) {
		super(message, cause);
	}

	public GatewayTimeout(String message) {
		super(message);
	}

	public GatewayTimeout(Throwable cause) {
		super(cause);
	}

	@Override
	public int getStatusCode() {
		return 504;
	}

	@Override
	public void printTo(PrintWriter writer) {
		writer.write(getMessage());
	}

}
