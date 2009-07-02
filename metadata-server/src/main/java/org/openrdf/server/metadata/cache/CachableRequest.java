package org.openrdf.server.metadata.cache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CachableRequest extends HttpServletRequestWrapper {
	private Collection<String> hidden = Arrays.asList("If-None-Match",
			"If-Modified-Since", "If-Match", "If-Unmodified-Since");
	private Vector<String> empty = new Vector<String>();
	private String ifNoneMatch;
	private String lastModified;
	private Long longModified;

	public CachableRequest(HttpServletRequest request, CachedResponse stale, String ifNoneMatch)
			throws IOException {
		super(request);
		this.ifNoneMatch = ifNoneMatch;
		if (stale != null) {
			this.lastModified = stale.getHeader("Last-Modified");
			this.longModified = stale.getDateHeader("Last-Modified");
		}
	}

	@Override
	public String getMethod() {
		String method = super.getMethod();
		if (method.equals("HEAD"))
			return "GET";
		return method;
	}

	@Override
	public long getDateHeader(String name) {
		if (longModified != null && "If-Modified-Since".equalsIgnoreCase(name))
			return longModified;
		if (hidden.contains(name))
			return 0;
		return super.getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		if (ifNoneMatch != null && "If-None-Match".equalsIgnoreCase(name))
			return ifNoneMatch;
		if (lastModified != null && "If-Modified-Since".equalsIgnoreCase(name))
			return lastModified;
		if (hidden.contains(name))
			return null;
		return super.getHeader(name);
	}

	@Override
	public Enumeration getHeaders(String name) {
		if (ifNoneMatch != null && "If-None-Match".equalsIgnoreCase(name)) {
			Vector<String> list = new Vector<String>();
			list.add(ifNoneMatch);
			return list.elements();
		}
		if (lastModified != null && "If-Modified-Since".equalsIgnoreCase(name)) {
			Vector<String> list = new Vector<String>();
			list.add(lastModified);
			return list.elements();
		}
		if (hidden.contains(name))
			return empty.elements();
		return super.getHeaders(name);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getMethod()).append(' ').append(getRequestURI()).append("\n");
		if (ifNoneMatch != null) {
			sb.append("If-None-Match: ").append(ifNoneMatch).append("\n");
		}
		if (lastModified != null) {
			sb.append("If-Modified-Since: ").append(lastModified).append("\n");
		}
		Enumeration names = getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Enumeration headers = getHeaders(name);
			while (headers.hasMoreElements()) {
				String value = (String) headers.nextElement();
				sb.append(name).append(": ").append(value).append("\n");
			}
		}
		sb.append("\n");
		return sb.toString();
	}

}
