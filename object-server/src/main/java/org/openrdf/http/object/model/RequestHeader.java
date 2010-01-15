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
package org.openrdf.http.object.model;

import info.aduna.net.ParsedURI;

import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openrdf.http.object.filters.IndentityPathFilter;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class RequestHeader {
	private HttpServletRequest request;
	private boolean unspecifiedVary;
	private String uri;
	private List<String> vary = new ArrayList<String>();

	public RequestHeader(HttpServletRequest request) {
		this.request = request;
		uri = request.getRequestURI();
		if (uri.startsWith("/")) {
			String host = getAuthority();
			try {
				String scheme = request.getScheme().toLowerCase();
				String path = getPath();
				java.net.URI net = new java.net.URI(scheme, host, path, null);
				uri = net.toASCIIString();
			} catch (URISyntaxException e) {
				// bad Host header
				StringBuffer url1 = request.getRequestURL();
				int idx = url1.indexOf("?");
				if (idx > 0) {
					uri = url1.substring(0, idx);
				} else {
					uri = url1.toString();
				}
			}
		}
	}

	public String getContentType() {
		return request.getContentType();
	}

	public long getDateHeader(String name) {
		return request.getDateHeader(name);
	}

	public String resolve(String url) {
		if (url == null)
			return null;
		return parseURI(url).toString();
	}

	public String getResolvedHeader(String name) {
		String value = request.getHeader(name);
		if (value == null)
			return null;
		return resolve(value);
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	public X509Certificate getX509Certificate() {
		return (X509Certificate) request
				.getAttribute("javax.servlet.request.X509Certificate");
	}

	public Enumeration getHeaderNames() {
		unspecifiedVary = true;
		return request.getHeaderNames();
	}

	public Enumeration getVaryHeaders(String name) {
		if (!vary.contains(name)) {
			vary.add(name);
		}
		return request.getHeaders(name);
	}

	public Enumeration getHeaders(String name) {
		return request.getHeaders(name);
	}

	public int getMaxAge() {
		return getCacheControl("max-age", Integer.MAX_VALUE);
	}

	public int getMinFresh() {
		return getCacheControl("min-fresh", 0);
	}

	public int getMaxStale() {
		return getCacheControl("max-stale", 0);
	}

	public boolean isStorable() {
		boolean safe = isSafe();
		return safe && !isMessageBody() && getCacheControl("no-store", 0) == 0;
	}

	public boolean isSafe() {
		String method = getMethod();
		return method.equals("HEAD") || method.equals("GET")
				|| method.equals("OPTIONS") || method.equals("PROFIND");
	}

	public boolean invalidatesCache() {
		String method = getMethod();
		return !isSafe() && !method.equals("TRACE") && !method.equals("COPY")
				&& !method.equals("LOCK") && !method.equals("UNLOCK");
	}

	public boolean isNoCache() {
		return isStorable() && getCacheControl("no-cache", 0) > 0;
	}

	public boolean isOnlyIfCache() {
		return isStorable() && getCacheControl("only-if-cached", 0) > 0;
	}

	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getRequestTarget() {
		Object value = request.getAttribute(IndentityPathFilter.ORIGINAL_REQUEST_TARGET);
		if (value != null)
			return value.toString();
		String qs = request.getQueryString();
		if (qs == null)
			return request.getRequestURI();
		return request.getRequestURI() + "?" + request.getQueryString();
	}

	public String getRequestURL() {
		String qs = request.getQueryString();
		if (qs == null)
			return uri;
		return uri + "?" + qs;
	}

	public String getURI() {
		return uri;
	}

	public ParsedURI parseURI(String uriSpec) {
		ParsedURI base = new ParsedURI(uri);
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		return base.resolve(uri);
	}

	public List<String> getVary() {
		if (unspecifiedVary)
			return Collections.singletonList("*");
		return vary;
	}

	public boolean isMessageBody() {
		return getHeader("Content-Length") != null
				|| getHeader("Transfer-Encoding") != null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getMethod()).append(" ").append(getRequestURL());
		return sb.toString();
	}

	public String getAuthority() {
		String uri = request.getRequestURI();
		if (uri != null && !uri.equals("*") && !uri.startsWith("/")) {
			try {
				return new java.net.URI(uri).getAuthority();
			} catch (URISyntaxException e) {
				// try the host header
			}
		}
		String host = request.getHeader("Host");
		if (host != null)
			return host.toLowerCase();
		if (request.getServerName() != null) {
			int port = request.getServerPort();
			if (port == 80 && "http".equals(request.getScheme()))
				return request.getServerName().toLowerCase();
			if (port == 443 && "https".equals(request.getScheme()))
				return request.getServerName();
			return request.getServerName().toLowerCase() + ":" + port;
		}
		return null;
	}

	public String getPath() {
		String path = request.getRequestURI();
		if (path == null || path.equals("*"))
			return null;
		if (!path.startsWith("/")) {
			try {
				return new java.net.URI(path).getPath();
			} catch (URISyntaxException e) {
				return null;
			}
		}
		int idx = path.indexOf('?');
		if (idx > 0) {
			path = path.substring(0, idx);
		}
		return path;
	}

	private int getCacheControl(String directive, int def) {
		Enumeration headers = getHeaders("Cache-Control");
		while (headers.hasMoreElements()) {
			String value = (String) headers.nextElement();
			for (String v : value.split("\\s*,\\s*")) {
				int idx = v.indexOf('=');
				if (idx >= 0 && directive.equals(v.substring(0, idx))) {
					try {
						return Integer.parseInt(v.substring(idx + 1));
					} catch (NumberFormatException e) {
						// invalid number
					}
				} else if (directive.equals(v)) {
					return Integer.MAX_VALUE;
				}
			}
		}
		return def;
	}

}