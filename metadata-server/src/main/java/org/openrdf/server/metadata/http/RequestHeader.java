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
package org.openrdf.server.metadata.http;

import info.aduna.net.ParsedURI;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;

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
		String host = getAuthority();
		try {
			String scheme = request.getScheme();
			String path = getPath();
			uri = new java.net.URI(scheme, host, path, null).toASCIIString();
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

	public Collection<? extends MimeType> getAcceptable()
			throws MimeTypeParseException {
		StringBuilder sb = new StringBuilder();
		Enumeration headers = getVaryHeaders("Accept");
		if (!headers.hasMoreElements())
			return Collections.singleton(new MimeType("*/*"));
		while (headers.hasMoreElements()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append((String) headers.nextElement());
		}
		String[] mediaTypes = sb.toString().split(",\\s*");
		Set<MimeType> list = new TreeSet<MimeType>(new Comparator<MimeType>() {

			public int compare(MimeType o1, MimeType o2) {
				String s1 = o1.getParameter("q");
				String s2 = o2.getParameter("q");
				Double q1 = s1 == null ? 1 : Double.valueOf(s1);
				Double q2 = s2 == null ? 1 : Double.valueOf(s2);
				int compare = q2.compareTo(q1);
				if (compare != 0)
					return compare;
				if (!"*".equals(o1.getPrimaryType()) && "*".equals(o2.getPrimaryType()))
					return -1;
				if ("*".equals(o1.getPrimaryType()) && !"*".equals(o2.getPrimaryType()))
					return 1;
				if (!"*".equals(o1.getSubType()) && "*".equals(o2.getSubType()))
					return -1;
				if ("*".equals(o1.getSubType()) && !"*".equals(o2.getSubType()))
					return 1;
				return o1.toString().compareTo(o2.toString());
			}
		});
		for (String mediaType : mediaTypes) {
			if (mediaType.startsWith("*;") || mediaType.equals("*")) {
				list.add(new MimeType("*/" + mediaType));
			} else {
				list.add(new MimeType(mediaType));
			}
		}
		return list;
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
		int maxage = getCacheControl("max-age");
		if (maxage == 0)
			return Integer.MAX_VALUE;
		return maxage;
	}

	public int getMinFresh() {
		return getCacheControl("min-fresh");
	}

	public int getMaxStale() {
		return getCacheControl("max-stale");
	}

	public boolean isStorable() {
		boolean safe = isSafe();
		return safe && !isMessageBody() && getCacheControl("no-store") == 0;
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
		return isStorable() && getCacheControl("no-cache") > 0;
	}

	public boolean isOnlyIfCache() {
		return isStorable() && getCacheControl("only-if-cached") > 0;
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getRequestURI() {
		return request.getRequestURI();
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
		String host = request.getHeader("Host");
		if (host == null) {
			int port = request.getServerPort();
			if (port == 80 && "http".equals(request.getScheme()))
				return request.getServerName();
			if (port == 443 && "https".equals(request.getScheme()))
				return request.getServerName();
			return request.getServerName() + ":" + port;
		}
		return host;
	}

	public String getPath() {
		String path = request.getRequestURI();
		int idx = path.indexOf('?');
		if (idx > 0) {
			path = path.substring(0, idx);
		}
		return path;
	}

	private int getCacheControl(String directive) {
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
		return 0;
	}

}
