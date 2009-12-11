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
package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Compresses the HTTP message response if appropriate.
 */
public class GZipResponse extends HttpServletResponseWrapper {
	private HttpServletResponse response;
	private boolean compressable;
	private String encoding;
	private boolean transformable = true;
	private String md5;
	private ServletOutputStream out;
	private int length = -1;
	private String size;
	private boolean head;

	public GZipResponse(HttpServletResponse response, boolean head) {
		super(response);
		this.response = response;
		this.head = head;
	}

	@Override
	public void setHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("Content-MD5".equalsIgnoreCase(name)) {
			md5 = value;
		} else if ("Content-Encoding".equalsIgnoreCase(name)) {
			encoding = value;
		} else if ("Content-Type".equalsIgnoreCase(name)) {
			setContentType(value);
		} else if ("Cache-Control".equalsIgnoreCase(name)) {
			if (value.contains("no-transform")) {
				transformable = false;
			}
			super.setHeader(name, value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void addHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("Content-MD5".equalsIgnoreCase(name)) {
			md5 = value;
		} else if ("Content-Encoding".equalsIgnoreCase(name)) {
			encoding = value;
		} else if ("Content-Type".equalsIgnoreCase(name)) {
			setContentType(value);
		} else if ("Cache-Control".equalsIgnoreCase(name)) {
			if (value.contains("no-transform")) {
				transformable = false;
			}
			super.addHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			length = value;
		} else {
			super.addIntHeader(name, value);
		}
	}

	@Override
	public void setContentLength(int len) {
		length = len;
	}

	@Override
	public void setContentType(String type) {
		compressable = type.startsWith("text/")
				|| type.startsWith("application/xml")
				|| type.startsWith("application/x-turtle")
				|| type.startsWith("application/trix")
				|| type.startsWith("application/x-trig")
				|| type.startsWith("application/postscript")
				|| type.startsWith("application/")
				&& (type.endsWith("+xml") || type.contains("+xml;"));
		super.setContentType(type);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (isCompressable()) {
			if (out == null) {
				response.setHeader("Content-Encoding", "gzip");
				ServletOutputStream stream = super.getOutputStream();
				out = new OutputServletStream(new GZIPOutputStream(stream));
			}
			return out;
		} else {
			flush();
			return response.getOutputStream();
		}
	}

	private boolean isCompressable() {
		return compressable && transformable && (encoding == null || "identity".equals(encoding));
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void flush() throws IOException {
		if (out != null) {
			out.flush();
		} else if (head && isCompressable()) {
			response.setHeader("Content-Encoding", "gzip");
		} else {
			if (encoding != null) {
				response.setHeader("Content-Encoding", encoding);
				encoding = null;
			}
			if (md5 != null) {
				response.setHeader("Content-MD5", md5);
				md5 = null;
			}
			if (size != null) {
				response.setHeader("Content-Length", size);
				size = null;
			} else if (length > -1) {
				response.setContentLength(length);
				length = -1;
			}
		}
	}

	@Override
	public void flushBuffer() throws IOException {
		flush();
		super.flushBuffer();
	}

}
