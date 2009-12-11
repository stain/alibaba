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
package org.openrdf.http.object.cache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps a request that will have its response cached for later use.
 */
public class CachableRequest extends HttpServletRequestWrapper {
	private Collection<String> hidden = Arrays.asList("If-None-Match",
			"If-Modified-Since", "If-Match", "If-Unmodified-Since", "If-Range",
			"Range");
	private Vector<String> empty = new Vector<String>();
	private String ifNoneMatch;
	private String lastModified;
	private Long longModified;

	public CachableRequest(HttpServletRequest request, CachedEntity stale,
			String ifNoneMatch) throws IOException {
		super(request);
		if (ifNoneMatch != null && ifNoneMatch.length() > 0) {
			this.ifNoneMatch = ifNoneMatch;
		}
		if (stale != null) {
			this.lastModified = stale.getLastModified();
			this.longModified = stale.lastModified();
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
