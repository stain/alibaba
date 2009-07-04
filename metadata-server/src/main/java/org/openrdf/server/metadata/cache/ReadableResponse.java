package org.openrdf.server.metadata.cache;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ReadableResponse extends HttpServletResponseWrapper {
	private Integer status;
	private String statusText;
	private long lastModified;
	private long date;
	private Map<String, String> headers = new LinkedHashMap<String, String>();

	public ReadableResponse(HttpServletResponse resp) {
		super(resp);
	}

	public Integer getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText;
	}

	public String getHeader(String name) {
		return headers.get(name.toLowerCase());
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getDate() {
		return date;
	}

	public void addDateHeader(String name, long value) {
		setDateHeader(name, value);
	}

	public void addHeader(String name, String value) {
		String key = name.toLowerCase();
		String existing = headers.get(key);
		if (existing == null) {
			headers.put(key, value);
		} else {
			headers.put(key, existing + "," + value);
		}
		super.addHeader(name, value);
	}

	public void addIntHeader(String name, int value) {
		addHeader(name, String.valueOf(value));
	}

	public void sendError(int sc, String msg) throws IOException {
		this.status = sc;
		this.statusText = msg;
		super.sendError(sc, msg);
	}

	public void sendError(int sc) throws IOException {
		this.status = sc;
		super.sendError(sc);
	}

	public void sendRedirect(String location) throws IOException {
		this.status = 307;
		headers.put("location", location);
		super.sendRedirect(location);
	}

	public void setDateHeader(String name, long value) {
		if ("Last-Modified".equalsIgnoreCase(name)) {
			lastModified = value;
		} else if ("Date".equalsIgnoreCase(name)) {
			date = value;
		} else {
			throw new IllegalArgumentException();
		}
		super.setDateHeader(name, value);
	}

	public void setHeader(String name, String value) {
		headers.put(name.toLowerCase(), value);
		super.setHeader(name, value);
	}

	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	public void setStatus(int sc, String sm) {
		this.status = sc;
		this.statusText = sm;
		super.setStatus(sc, sm);
	}

	public void setStatus(int sc) {
		this.status = sc;
		super.setStatus(sc);
	}

	public void setContentLength(int len) {
		setIntHeader("Content-Length", len);
	}

	public void setContentType(String type) {
		setHeader("Content-Type", type);
	}

	public void setLocale(Locale locale) {
		String value = locale.getLanguage();
		if (value != null && value.length() > 0) {
			String country = locale.getCountry();
			StringBuffer sb = new StringBuffer(value);
			if (country != null && country.length() > 0) {
				sb.append('-');
				sb.append(country);
			}
			value = sb.toString();
		}
		setHeader("Content-Language", value);
	}

	public boolean containsHeader(String name) {
		return headers.containsKey(name.toLowerCase());
	}

	public String getContentType() {
		return headers.get("content-type");
	}

	public Locale getLocale() {
		String lang = headers.get("content-language");
		if (lang == null)
			return null;
		int idx = lang.indexOf('-');
		if (idx > 0) {
			return new Locale(lang.substring(0, idx), lang.substring(idx + 1));
		} else {
			return new Locale(lang);
		}
	}

}
