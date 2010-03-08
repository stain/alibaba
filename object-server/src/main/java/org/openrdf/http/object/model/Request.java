/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.openrdf.http.object.exceptions.BadRequest;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class Request extends EditableHttpEntityEnclosingRequest {
	private long received = System.currentTimeMillis();

	public Request(HttpRequest request) {
		super(request);
	}

	public long getReceivedOn() {
		return received;
	}

	public void setReceivedOn(long received) {
		this.received = received;
	}

	@Override
	public Request clone() {
		Request clone = (Request) super.clone();
		clone.received = received;
		return clone;
	}

	public String getHeader(String name) {
		Header[] headers = getHeaders(name);
		if (headers == null || headers.length == 0)
			return null;
		return headers[0].getValue();
	}

	public long getDateHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return -1;
		try {
			return DateUtil.parseDate(value).getTime();
		} catch (DateParseException e) {
			return -1;
		}
	}

	public String resolve(String url) {
		if (url == null)
			return null;
		return parseURI(url).toString();
	}

	public String getResolvedHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return null;
		return resolve(value);
	}

	public X509Certificate getX509Certificate() {
		// TODO getAttribute("javax.servlet.request.X509Certificate");
		return null;
	}

	public String getRemoteAddr() {
		// TODO REMOTE_ADDR
		return null;
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

	public String getMethod() {
		return getRequestLine().getMethod();
	}

	public String getRequestTarget() {
		Object value = null;
		// TODO
		// request.getAttribute(IndentityPathFilter.ORIGINAL_REQUEST_TARGET);
		if (value != null)
			return value.toString();
		return getRequestLine().getUri();
	}

	public String getQueryString() {
		String qs = getRequestLine().getUri();
		int idx = qs.indexOf('?');
		if (idx < 0)
			return null;
		return qs.substring(idx + 1);
	}

	public String getRequestURL() {
		String qs = getQueryString();
		if (qs == null)
			return getURI();
		return getURI() + "?" + qs;
	}

	public String getURI() {
		String uri = getRequestLine().getUri();
		if (uri.indexOf('?') > 0) {
			uri = uri.substring(0, uri.indexOf('?'));
		}
		if (uri.startsWith("/")) {
			String scheme = getScheme().toLowerCase();
			String host = getAuthority();
			String path = getPath();
			try {
				java.net.URI net;
				int idx = host.indexOf(':');
				if (idx > 0) {
					String hostname = host.substring(0, idx);
					int port = Integer.parseInt(host.substring(idx + 1));
					net = new java.net.URI(scheme, null, hostname, port, path,
							null, null);
				} else {
					net = new java.net.URI(scheme, host, path, null);
				}
				uri = net.toASCIIString();
			} catch (URISyntaxException e) {
				// bad Host header
				throw new BadRequest(e.getMessage());
			}
		}
		return uri;
	}

	public ParsedURI parseURI(String uriSpec) {
		ParsedURI base = new ParsedURI(getURI());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		return base.resolve(uri);
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
		String uri = getRequestLine().getUri();
		if (uri != null && !uri.equals("*") && !uri.startsWith("/")) {
			try {
				return new java.net.URI(uri).getAuthority();
			} catch (URISyntaxException e) {
				// try the host header
			}
		}
		String host = getHeader("Host");
		if (host != null)
			return host.toLowerCase();
		throw new BadRequest("Missing Host Header");
	}

	public String getPath() {
		String path = getRequestLine().getUri();
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

	private String getScheme() {
		// TODO compute scheme
		return "http";
	}

	protected Enumeration getHeaderEnumeration(String name) {
		Vector values = new Vector();
		for (Header hd : getHeaders(name)) {
			values.add(hd.getValue());
		}
		return values.elements();
	}

	private int getCacheControl(String directive, int def) {
		Enumeration headers = getHeaderEnumeration("Cache-Control");
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
