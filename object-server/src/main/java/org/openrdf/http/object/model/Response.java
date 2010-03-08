/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.AbstractHttpMessage;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.Conflict;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.http.object.exceptions.NotFound;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;

/**
 * Builds an HTTP response.
 * 
 * @author James Leigh
 */
public class Response extends AbstractHttpMessage {
	/** Date format pattern used to generate the header in RFC 1123 format. */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
	/** The time zone to use in the date header. */
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final DateFormat dateformat;
	static {
		dateformat = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
		dateformat.setTimeZone(GMT);
	}

	private ResponseEntity entity;
	private ResponseException exception;
	private boolean head;
	private long lastModified;
	private Class<?> type;
	private int status = 204;
	private String phrase = "No Content";
	private List<Runnable> onclose = new LinkedList<Runnable>();

	public Response() {
		setHeader("Content-Length", "0");
	}

	public Response onClose(Runnable task) {
		onclose.add(task);
		return this;
	}

	public List<Runnable> getOnClose() {
		return onclose;
	}

	public Response unauthorized(InputStream message) throws IOException {
		status(401, "Unauthorized");
		if (message == null)
			return this;
		StringWriter headers = new StringWriter();
		BufferedInputStream in = new BufferedInputStream(message);
		int ch = 0;
		while (true) {
			ch = in.read();
			if (ch == -1)
				break;
			in.mark(3);
			if (ch == '\r' && in.read() == '\n' && in.read() == '\r'
					&& in.read() == '\n') {
				break;
			} else {
				in.reset();
				headers.write(ch);
			}
		}
		String[] mimeTypes = new String[0];
		for (String header : headers.toString().split("\r\n")) {
			String lc = header.toLowerCase();
			String value = header.substring(header.indexOf(':') + 1).trim();
			if (lc.startsWith("www-authenticate:")) {
				header("WWW-Authenticate", value);
			} else if (lc.startsWith("content-type:")) {
				header("Content-Type", value);
				mimeTypes = new String[] { value };
			}
		}
		this.type = InputStream.class;
		this.entity = new ResponseEntity(mimeTypes, in, type, type, null, null);
		removeHeaders("Content-Length");
		return this;
	}

	public Response exception(ResponseException e) {
		status(e.getStatusCode(), e.getMessage());
		this.exception = e;
		this.entity = null;
		setHeader("Content-Length", "0");
		return this;
	}

	public Response badRequest(Exception e) {
		return exception(new BadRequest(e));
	}

	public Response conflict(ConcurrencyException e) {
		return exception(new Conflict(e));
	}

	public Response entity(ResponseEntity entity) {
		status(200, "OK");
		this.entity = entity;
		removeHeaders("Content-Length");
		return this;
	}

	public ResponseEntity getResponseEntity() {
		return entity;
	}

	public Class<?> getEntityType() {
		return type;
	}

	public ResponseException getException() {
		return exception;
	}

	public String getHeader(String header) {
		Header hd = getFirstHeader(header);
		if (hd == null)
			return null;
		return hd.getValue();
	}

	public Long getLastModified() {
		return lastModified;
	}

	public long getSize(String mimeType, Charset charset) {
		return entity.getSize(mimeType, charset);
	}

	public int getStatus() {
		return getStatusCode();
	}

	public String getMessage() {
		return phrase;
	}

	public Response head() {
		head = true;
		return this;
	}

	public Response header(String header, String value) {
		if (value == null) {
			removeHeaders(header);
		} else {
			String existing = getHeader(header);
			if (existing == null) {
				setHeader(header, value);
			} else if (!existing.equals(value)) {
				setHeader(header, existing + "," + value);
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
		return getStatusCode() == 204;
	}

	public boolean isOk() {
		return getStatusCode() == 200;
	}

	public Response lastModified(long lastModified) {
		if (lastModified <= 0)
			return this;
		lastModified = lastModified / 1000 * 1000;
		long pre = this.lastModified;
		if (pre >= lastModified)
			return this;
		this.lastModified = lastModified;
		setDateHeader("Last-Modified", lastModified);
		return this;
	}

	public Response location(String location) {
		header("Location", location);
		return this;
	}

	public Response noContent() {
		status(204, "No Content");
		this.entity = null;
		setHeader("Content-Length", "0");
		return this;
	}

	public Response notFound() {
		return exception(new NotFound());
	}

	public Response notModified() {
		status(304, "Not Modified");
		this.entity = null;
		setHeader("Content-Length", "0");
		return this;
	}

	public Response preconditionFailed() {
		status(412, "Precondition Failed");
		this.entity = null;
		setHeader("Content-Length", "0");
		return this;
	}

	public Response server(Exception error) {
		return exception(new InternalServerError(error));
	}

	public void setEntityType(Class<?> type) {
		this.type = type;
	}

	public Response status(int status, String msg) {
		this.status = status;
		this.phrase = msg;
		return this;
	}

	public String toString() {
		return phrase;
	}

	public InputStream write(String mimeType, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		return entity.write(mimeType, charset);
	}

	public void setDateHeader(String name, long time) {
		header(name, dateformat.format(time));
	}

	public int getStatusCode() {
		return status;
	}

	public ProtocolVersion getProtocolVersion() {
		return new ProtocolVersion("HTTP", 1, 1);
	}

}
