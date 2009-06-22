/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.server.metadata.http;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.server.metadata.concepts.RDFResource;

/**
 * Builds an HTTP response.
 * 
 * @author James Leigh
 */
public class Response {
	private String contentType = "*/*;q=0.001";
	private Map<String, Long> dateHeaders = new HashMap<String, Long>();
	private Object entity;
	private boolean head;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private int status = 204;

	public Response badRequest() {
		this.status = 400;
		return this;
	}

	public Response badRequest(Exception e) {
		this.status = 400;
		this.entity = e;
		return this;
	}

	public Response client(Exception error) {
		this.status = 400;
		this.entity = error;
		return this;
	}

	public Response entity(Object entity) {
		if (entity == null)
			return noContent();
		this.status = 200;
		this.entity = entity;
		return this;
	}

	public Response entity(File entity, RDFResource target) {
		this.status = 200;
		this.entity = entity;
		header("ETag", target.eTag());
		long m = target.lastModified();
		long lastModified = entity.lastModified();
		if (m > lastModified) {
			lastModified = m;
		}
		return lastModified(lastModified);
	}

	public Response entity(Object entity, RDFResource target) {
		eTag(target);
		if (entity == null)
			return noContent();
		this.status = 200;
		this.entity = entity;
		return this;
	}

	public Response eTag(RDFResource target) {
		header("ETag", target.eTag());
		return lastModified(target.lastModified());
	}

	public String getContentType() {
		return contentType;
	}

	public Long getDateHeader(String header) {
		return dateHeaders.get(header);
	}

	public Object getEntity() {
		return entity;
	}

	public Set<String> getHeaderNames() {
		return headers.keySet();
	}

	public List<String> getHeaders(String header) {
		return headers.get(header);
	}

	public int getStatus() {
		return status;
	}

	public Response head() {
		head = true;
		return this;
	}

	public Response header(String header, int value) {
		return header(header, String.valueOf(value));
	}

	public Response header(String header, long value) {
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

	public boolean isHead() {
		return head;
	}

	public boolean isNoContent() {
		return status == 204;
	}

	public boolean isOk() {
		return status == 200;
	}

	public Response lastModified(long lastModified) {
		if (lastModified <= 0)
			return this;
		lastModified = lastModified / 1000 * 1000;
		if (headers.containsKey("Last-Modified")) {
			long pre = dateHeaders.get("Last-Modified");
			if (pre >= lastModified)
				return this;
		} else {
			headers.put("Last-Modified", new ArrayList<String>());
		}
		dateHeaders.put("Last-Modified", lastModified);
		return this;
	}

	public Response location(String location) {
		header("Location", location);
		return this;
	}

	public Response noContent() {
		this.status = 204;
		this.entity = null;
		return this;
	}

	public Response notFound() {
		this.status = 404;
		this.entity = "Not Found";
		return this;
	}

	public Response notFound(String message) {
		this.entity = message;
		this.status = 404;
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

	public Response server(Exception error) {
		this.status = 500;
		this.entity = error;
		return this;
	}

	public Response status(int status) {
		this.status = status;
		return this;
	}

	public Response type(String value) {
		contentType = value;
		return this;
	}

	public Response conflict(ConcurrencyException e) {
		this.status = 409;
		this.entity = e;
		return this;
	}

}
