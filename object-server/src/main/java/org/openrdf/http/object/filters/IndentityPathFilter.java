/*
 * Copyright 2010, Zepheira Some rights reserved.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Extracts a percent encoded URI from the URL path.
 * 
 * @author James Leigh
 *
 */
public class IndentityPathFilter implements Filter {
	private static final class DivertedRequest extends
			HttpServletRequestWrapper {
		private final String uri;

		private DivertedRequest(HttpServletRequest request, String uri) {
			super(request);
			this.uri = uri;
		}

		public String getRequestURI() {
			return uri;
		}

		public StringBuffer getRequestURL() {
			String qs = getQueryString();
			StringBuffer sb = new StringBuffer();
			sb.append(getRequestURI());
			if (qs != null) {
				sb.append('?').append(qs);
			}
			return sb;
		}

		@Override
		public Object getAttribute(String name) {
			Object value = super.getAttribute(name);
			if (value == null && ORIGINAL_REQUEST_TARGET.equals(name)) {
				if (getQueryString() == null)
					return super.getRequestURI();
				return super.getRequestURI() + "?" + getQueryString();
			}
			return value;
		}
	}

	private final static class DivertURIResponse extends
			HttpServletResponseWrapper {
		private final String from;
		private final String to;

		private DivertURIResponse(HttpServletResponse response, String from,
				String to) {
			super(response);
			this.from = from;
			this.to = to;
		}

		public void addHeader(String name, String value) {
			if (name != null && name.equalsIgnoreCase("Location")) {
				super.addHeader(name, divert(value));
			} else {
				super.addHeader(name, value);
			}
		}

		public void setHeader(String name, String value) {
			if (name != null && name.equalsIgnoreCase("Location")) {
				super.setHeader(name, divert(value));
			} else {
				super.setHeader(name, value);
			}
		}

		public String encodeRedirectUrl(String url) {
			return encodeRedirectURL(url);
		}

		public String encodeRedirectURL(String url) {
			return super.encodeRedirectURL(divert(url));
		}

		public String encodeUrl(String url) {
			return encodeURL(url);
		}

		public String encodeURL(String url) {
			return super.encodeURL(divert(url));
		}

		private String divert(String url) {
			if (url == null)
				return url;
			int idx = url.lastIndexOf('?');
			if (idx < 0 && url.equals(from))
				return to;
			if (idx >= 0 && url.substring(0, idx).equals(from))
				return to + url.substring(idx);
			return url;
		}
	}

	public static final String ORIGINAL_REQUEST_TARGET = "X-Original-Request-TARGET";
	private String prefix;

	public String getIdentityPathPrefix() {
		return prefix;
	}

	public void setIdentityPathPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void init(FilterConfig config) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		try {
			URI net = getRequestTargetWithoutQueryString(req);
			String path = net.getPath();
			if (prefix != null && path != null && path.startsWith(prefix)) {
				String encoded = path.substring(prefix.length());
				String uri = URLDecoder.decode(encoded, "UTF-8");
				req = new DivertedRequest(req, uri);
				resp = new DivertURIResponse(resp, uri, net.toASCIIString());
			}
		} catch (URISyntaxException e) {
			// unrecognisable request URI
		}
		chain.doFilter(req, resp);
	}

	private URI getRequestTargetWithoutQueryString(HttpServletRequest req)
			throws URISyntaxException {
		String path = req.getRequestURI();
		if ("*".equals(path)) {
			path = null;
		}
		if (path != null && !path.equals("*") && !path.startsWith("/")) {
			try {
				return new URI(path);
			} catch (URISyntaxException e) {
				path = null;
			}
		}
		try {
			String scheme = req.getScheme().toLowerCase();
			String authority = getAuthority(req);
			return new URI(scheme, authority, path, null);
		} catch (URISyntaxException e) {
			// bad Host header
			return new URI(req.getRequestURL().toString());
		}
	}

	private String getAuthority(HttpServletRequest request) {
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

}
