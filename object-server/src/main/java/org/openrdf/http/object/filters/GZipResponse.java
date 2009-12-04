package org.openrdf.http.object.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

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
