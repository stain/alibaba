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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.openrdf.server.metadata.exceptions.Conflict;
import org.openrdf.server.metadata.exceptions.InternalServerError;
import org.openrdf.server.metadata.exceptions.NotFound;
import org.openrdf.server.metadata.exceptions.ResponseException;

/**
 * Builds an HTTP response.
 * 
 * @author James Leigh
 */
public class Response {
	private ResponseEntity entity;
	private ResponseException exception;
	private boolean head;
	private Map<String, String> headers = new HashMap<String, String>();
	private long lastModified;
	private int status = 204;
	private String msg;
	private Class<?> type;

	public Response exception(ResponseException e) {
		this.status = e.getStatusCode();
		this.msg = e.getMessage();
		this.exception = e;
		this.entity = null;
		return this;
	}

	public Response badRequest(Exception e) {
		return exception(new BadRequest(e));
	}

	public Response conflict(ConcurrencyException e) {
		return exception(new Conflict(e));
	}

	public Response entity(ResponseEntity entity) {
		this.status = 200;
		this.entity = entity;
		return this;
	}

	public Response file(FileEntity entity) {
		this.status = 200;
		this.entity = entity;
		return this;
	}

	public ResponseEntity getEntity() {
		return entity;
	}

	public Class<?> getEntityType() {
		return type;
	}

	public ResponseException getException() {
		return exception;
	}

	public String getHeader(String header) {
		return headers.get(header);
	}

	public Set<String> getHeaderNames() throws MimeTypeParseException {
		return headers.keySet();
	}

	public Long getLastModified() {
		return lastModified;
	}

	public long getSize(String mimeType, Charset charset) {
		return entity.getSize(mimeType, charset);
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return msg;
	}

	public Response head() {
		head = true;
		return this;
	}

	public Response header(String header, String value) {
		if (value == null) {
			headers.remove(header);
		} else {
			String existing = headers.get(header);
			if (existing == null) {
				headers.put(header, value);
			} else {
				headers.put(header, existing + "," + value);
			}
		}
		return this;
	}

	public boolean isContent() {
		return entity != null || exception != null;
	}

	public boolean isException() {
		return exception != null;
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
		long pre = this.lastModified;
		if (pre >= lastModified)
			return this;
		this.lastModified = lastModified;
		return this;
	}

	public Response location(String location) {
		header("Location", location);
		return this;
	}

	public Response noContent() {
		this.status = 204;
		this.msg = "No Content";
		this.entity = null;
		return this;
	}

	public Response notFound() {
		return exception(new NotFound());
	}

	public Response notModified() {
		this.status = 304;
		this.msg = "Not Modified";
		this.entity = null;
		return this;
	}

	public Response preconditionFailed() {
		this.status = 412;
		this.msg = "Precondition Failed";
		this.entity = null;
		return this;
	}

	public Response server(Exception error) {
		return exception(new InternalServerError(error));
	}

	public void setEntityType(Class<?> type) {
		this.type = type;
	}

	public Response status(int status) {
		this.status = status;
		return this;
	}

	public Response status(String status) {
		this.msg = status;
		return this;
	}

	public String toString() {
		return Integer.toString(status);
	}

	public void writeTo(String mimeType, Charset charset, OutputStream out,
			int bufSize) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		entity.writeTo(mimeType, charset, out, bufSize);
	}

}
