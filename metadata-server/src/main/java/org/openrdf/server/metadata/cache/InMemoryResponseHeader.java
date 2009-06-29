package org.openrdf.server.metadata.cache;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public abstract class InMemoryResponseHeader implements HttpServletResponse {
	private Integer status;
	private String statusText;
	private long lastModified;
	private long date;
	private Map<String, String> headers = new LinkedHashMap<String, String>();

	public InMemoryResponseHeader() {
	}

	public Integer getStatus() {
		return status;
	}

	public String getStatusText() {
		return statusText;
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

	public void addCookie(Cookie cookie) {
		throw new UnsupportedOperationException();
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
	}

	public void addIntHeader(String name, int value) {
		addHeader(name, String.valueOf(value));
	}

	public void sendError(int sc, String msg) throws IOException {
		this.status = sc;
		this.statusText = msg;
	}

	public void sendError(int sc) throws IOException {
		this.status = sc;
	}

	public void sendRedirect(String location) throws IOException {
		this.status = 307;
		headers.put("Location", location);
	}

	public void setDateHeader(String name, long value) {
		if ("Last-Modified".equalsIgnoreCase(name)) {
			lastModified = value;
		} else if ("Date".equalsIgnoreCase(name)) {
			date = value;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void setHeader(String name, String value) {
		headers.put(name.toLowerCase(), value);
	}

	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	public void setStatus(int sc, String sm) {
		this.status = sc;
		this.statusText = sm;
	}

	public void setStatus(int sc) {
		this.status = sc;
	}

	public void setCharacterEncoding(String charset) {
		throw new UnsupportedOperationException();
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
		return headers.containsKey(name);
	}

	public String getContentType() {
		return headers.get("Content-Type");
	}

	public Locale getLocale() {
		String lang = headers.get("Content-Language");
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
