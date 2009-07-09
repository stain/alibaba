package org.openrdf.server.metadata.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.server.metadata.filters.OutputServletStream;

public class FileResponse extends InMemoryResponseHeader {
	private static AtomicLong seq = new AtomicLong(0);
	private HttpServletResponse response;
	private boolean storable = true;
	private boolean privatecache;
	private String entityTag;
	private boolean notModified = false;
	private OutputServletStream out;
	private boolean committed = false;
	private String method;
	private String url;
	private File dir;
	private File file;
	private Lock lock;

	public FileResponse(String url, HttpServletRequest req, HttpServletResponse res,
			File dir, Lock lock) {
		this.method = req.getMethod();
		this.url = url;
		this.dir = dir;
		this.response = res;
		this.lock = lock;
		privatecache = req.getHeader("Authorization") != null;
	}

	public boolean isCachable() {
		if (storable && !privatecache && entityTag != null)
			return true;
		lock.release();
		return false;
	}

	public boolean isModified() {
		return !notModified;
	}

	public String getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	public String getEntityTag() {
		return entityTag;
	}

	public File getMessageBody() {
		return file;
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
			if (value.contains("public")) {
				privatecache = false;
			}
		} else if ("ETag".equalsIgnoreCase(name)) {
			int start = value.indexOf('"');
			int end = value.lastIndexOf('"');
			entityTag = value.substring(start + 1, end);
		}
		super.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		if ("Cache-Control".equalsIgnoreCase(name)) {
			if (value.contains("no-store") || value.contains("private")) {
				storable &= out != null;
			}
			if (value.contains("public")) {
				privatecache = false;
			}
			super.addHeader(name, value);
		} else if ("ETag".equalsIgnoreCase(name)) {
			setHeader(name, value);
		} else {
			super.addHeader(name, value);
		}
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (isCachable()) {
			assert isModified();
			if (out == null) {
				long id = seq.incrementAndGet();
				String hex = Integer.toHexString(url.hashCode());
				file = new File(dir, "$" + hex + '-' + id + ".part");
				dir.mkdirs();
				out = new OutputServletStream(new FileOutputStream(file));
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
		} else if (out != null) {
			out.flush();
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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getStatus()).append(' ').append(getStatusText()).append("\n");
		Map<String, String> map = getHeaders();
		long lastModified = getLastModified();
		long date = getDate();
		for (String header : map.keySet()) {
			sb.append(header).append(": ").append(map.get(header)).append("\n");
		}
		if (lastModified > 0) {
			sb.append("Last-Modified: ").append(new Date(lastModified)).append(
					"\n");
		}
		if (date > 0) {
			sb.append("Date: ").append(new Date(date)).append("\n");
		}
		sb.append("\n");
		return sb.toString();
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
