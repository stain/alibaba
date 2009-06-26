package org.openrdf.server.metadata.cache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class FileResponse extends InMemoryResponseHeader {
	private HttpServletResponse response;
	private boolean storable = true;
	private boolean validatable = false;
	private boolean notModified = false;
	private CachedResponse cache;
	private OutputServletStream out;
	private boolean committed = false;

	public FileResponse(CachedResponse cache, HttpServletResponse res) {
		this.cache = cache;
		this.response = res;
	}

	public boolean isCachable() {
		return storable && validatable;
	}

	public boolean isNotModified() {
		return notModified;
	}

	@Override
	public void setStatus(int sc, String sm) {
		if (sc == 412 || sc == 304) {
			notModified = true;
		}
		super.setStatus(sc, sm);
	}

	@Override
	public void setStatus(int sc) {
		if (sc == 412 || sc == 304) {
			notModified = true;
		}
		super.setStatus(sc);
	}

	@Override
	public void setHeader(String name, String value) {
		if ("Cache-Control".equalsIgnoreCase(name)) {
			if (value.contains("no-store") || value.contains("private")) {
				storable &= out != null;
			}
		} else if ("ETag".equalsIgnoreCase(name)
				|| "Last-Modified".equalsIgnoreCase(name)) {
			validatable = true;
		}
		super.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		if ("Cache-Control".equalsIgnoreCase(name)) {
			if (value.contains("no-store") || value.contains("private")) {
				storable &= out != null;
			}
		} else if ("ETag".equalsIgnoreCase(name)
				|| "Last-Modified".equalsIgnoreCase(name)) {
			validatable = true;
		}
		super.addHeader(name, value);
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (isCachable()) {
			assert !isNotModified();
			if (out == null) {
				out = new OutputServletStream(cache.createOutputStream());
			}
			return out;
		} else {
			flushHeaders();
			return response.getOutputStream();
		}
	}

	public PrintWriter getWriter() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void flushBuffer() throws IOException {
		if (!isCachable()) {
			flushHeaders();
			response.flushBuffer();
		} else if (!isNotModified()) {
			if (out != null) {
				out.close();
				String length = String.valueOf(cache.getContentLength());
				super.setHeader("Content-Length", length);
			}
			Integer status = getStatus();
			String statusText = getStatusText();
			Map<String, String> map = getHeaders();
			long lastModified = getLastModified();
			long date = getDate();
			cache.setStatus(status, statusText);
			for (String header : map.keySet()) {
				cache.setHeader(header, map.get(header));
			}
			if (lastModified > 0) {
				cache.setDateHeader("Last-Modified", lastModified);
			}
			if (date > 0) {
				cache.setDateHeader("Date", date);
			}
		}
	}

	public String encodeRedirectUrl(String arg0) {
		return response.encodeRedirectUrl(arg0);
	}

	public String encodeRedirectURL(String arg0) {
		return response.encodeRedirectURL(arg0);
	}

	public String encodeUrl(String arg0) {
		return response.encodeUrl(arg0);
	}

	public String encodeURL(String arg0) {
		return response.encodeURL(arg0);
	}

	public int getBufferSize() {
		return response.getBufferSize();
	}

	public boolean isCommitted() {
		return response.isCommitted();
	}

	public void setBufferSize(int arg0) {
		response.setBufferSize(arg0);
	}

	public String getCharacterEncoding() {
		throw new UnsupportedOperationException();
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public void resetBuffer() {
		throw new UnsupportedOperationException();
	}

	private void flushHeaders() {
		if (!committed) {
			committed = true;
			Integer status = getStatus();
			String statusText = getStatusText();
			Map<String, String> map = getHeaders();
			long lastModified = getLastModified();
			long date = getDate();
			if (statusText == null) {
				response.setStatus(status);
			} else {
				response.setStatus(status, statusText);
			}
			for (String header : map.keySet()) {
				response.setHeader(header, map.get(header));
			}
			if (lastModified > 0) {
				response.setDateHeader("Last-Modified", lastModified);
			}
			if (date > 0) {
				response.setDateHeader("Date", date);
			}
		}
	}

}
