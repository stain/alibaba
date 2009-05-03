package org.openrdf.server.metadata.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Response {
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private Map<String, Long> dateHeaders = new HashMap<String, Long>();
	private Object entity;
	private String contentType = "*/*;q=0.001";
	private int status = 204;
	private boolean head;

	public Response header(String header, long value) {
		return header(header, String.valueOf(value));
	}

	public Response header(String header, int value) {
		return header(header, String.valueOf(value));
	}

	public Response header(String header, String value) {
		if (value == null) {
			headers.remove(header);
		} else {
			List<String> list = headers.get(header);
			if (list == null) {
				headers.put(header, list = new ArrayList<String>());
			}
			list.add(value);
		}
		return this;
	}

	public Response type(String value) {
		contentType = value;
		return this;
	}

	public Response entity(Object entity) {
		this.status = 200;
		this.entity = entity;
		return this;
	}

	public Response server(Exception error) {
		this.status = 500;
		this.entity = error;
		return this;
	}

	public Response client(Exception error) {
		this.status = 400;
		this.entity = error;
		return this;
	}

	public Response notModified() {
		this.status = 304;
		this.entity = null;
		return this;
	}

	public Response preconditionFailed() {
		this.status = 412;
		this.entity = null;
		return this;
	}

	public Response noContent() {
		this.status = 204;
		this.entity = null;
		return this;
	}

	public boolean isOk() {
		return status == 200;
	}

	public boolean isNoContent() {
		return status == 204;
	}

	public Response head() {
		head = true;
		return this;
	}

	public Response status(int status) {
		this.status = status;
		return this;
	}

	public Response location(String location) {
		header("Location", location);
		return this;
	}

	public Set<String> getHeaderNames() {
		return headers.keySet();
	}

	public List<String> getHeaders(String header) {
		return headers.get(header);
	}

	public Long getDateHeader(String header) {
		return dateHeaders.get(header);
	}

	public Object getEntity() {
		return entity;
	}

	public int getStatus() {
		return status;
	}

	public String getContentType() {
		return contentType;
	}

	public Response notFound(String message) {
		this.entity = message;
		this.status = 404;
		return this;
	}

	public Response lastModified(long lastModified) {
		if (!headers.containsKey("Last-Modified")) {
			headers.put("Last-Modified", new ArrayList<String>());
		}
		dateHeaders.put("Last-Modified", lastModified);
		return this;
	}

	public boolean isHead() {
		return head;
	}

	public Response notFound() {
		this.status = 404;
		this.entity = "Not Found";
		return this;
	}

	public Response badRequest() {
		this.status = 400;
		return this;
	}

}
