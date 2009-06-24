package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GZipResponseWrapper extends HttpServletResponseWrapper {
	private HttpServletResponse response;
	private boolean compressed;
	private GZipServletStream out;
	private PrintWriter writer;
	private int length = -1;
	private String size;

	public GZipResponseWrapper(HttpServletResponse response) {
		super(response);
		this.response = response;
	}

	@Override
	public void addHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("Content-Type".equalsIgnoreCase(name)) {
			setContentType(value);
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
	public void setHeader(String name, String value) {
		if ("Content-Length".equalsIgnoreCase(name)) {
			size = value;
		} else if ("Content-Type".equalsIgnoreCase(name)) {
			setContentType(value);
		} else {
			super.setHeader(name, value);
		}
	}

	@Override
	public void setContentLength(int len) {
		length = len;
	}

	@Override
	public void setContentType(String type) {
		compressed = type.startsWith("text/")
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
		if (!compressed) {
			flush();
			return response.getOutputStream();
		}
		if (out == null) {
			out = new GZipServletStream(super.getOutputStream());
			response.addHeader("Content-Encoding", "gzip");
		}
		return out;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (!compressed) {
			flush();
			return response.getWriter();
		}
		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(getOutputStream(),
					"UTF-8"));
		}
		return writer;
	}

	public void flush() throws IOException {
		if (writer != null) {
			writer.flush();
		} else if (out != null) {
			out.flush();
		} else if (size != null) {
			response.setHeader("Content-Length", size);
			size = null;
		} else if (length > -1) {
			response.setContentLength(length);
			length = -1;
		}
	}

}
