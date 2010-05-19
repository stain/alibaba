package org.openrdf.http.object.mxbeans;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ConnectionBean implements Serializable {
	private static final long serialVersionUID = 332325306145674774L;
	private String status;
	private String request;
	private String response;
	private String consuming;
	private String[] pending;

	public ConnectionBean() {
		super();
	}

	@ConstructorProperties( { "status", "request", "response", "consuming",
			"pending" })
	public ConnectionBean(String status, String tcp, String request,
			String response, String consuming, String[] pending) {
		this.status = status;
		this.request = request;
		this.response = response;
		this.consuming = consuming;
		this.pending = pending;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getConsuming() {
		return consuming;
	}

	public void setConsuming(String consuming) {
		this.consuming = consuming;
	}

	public String[] getPending() {
		return pending;
	}

	public void setPending(String[] pending) {
		this.pending = pending;
	}
}
