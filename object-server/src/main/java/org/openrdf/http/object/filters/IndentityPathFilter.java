/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicRequestLine;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Request;

/**
 * Extracts a percent encoded URI from the URL path.
 * 
 * @author James Leigh
 *
 */
public class IndentityPathFilter extends Filter {
	private String prefix;

	public IndentityPathFilter(Filter delegate) {
		super(delegate);
	}

	public String getIdentityPathPrefix() {
		return prefix;
	}

	public void setIdentityPathPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Request filter(Request req) throws IOException {
		try {
			URI net = getRequestTargetWithoutQueryString(req);
			String path = net.getPath();
			if (prefix != null && path != null && path.startsWith(prefix)) {
				String encoded = path.substring(prefix.length());
				String uri = URLDecoder.decode(encoded, "UTF-8");
				RequestLine line = req.getRequestLine();
				String target = uri;
				int idx = line.getUri().indexOf('?');
				if (idx > 0) {
					target = uri + line.getUri().substring(idx);
				}
				String method = line.getMethod();
				ProtocolVersion version = line.getProtocolVersion();
				line = new BasicRequestLine(method, target, version);
				req.setRequestLine(line);
				// TODO make original request-target available, but not spoofable
			}
		} catch (URISyntaxException e) {
			// unrecognisable request URI
		}
		return super.filter(req);
	}

	public HttpResponse filter(Request req, HttpResponse resp)
			throws IOException {
		resp = super.filter(req, resp);
		try {
			URI net = getRequestTargetWithoutQueryString(req);
			String path = net.getPath();
			if (prefix != null && path != null && path.startsWith(prefix)) {
				String encoded = path.substring(prefix.length());
				String uri = URLDecoder.decode(encoded, "UTF-8");
				RequestLine line = req.getRequestLine();
				int idx = line.getUri().indexOf('?');
				Header hd = resp.getFirstHeader("Location");
				if (hd == null)
					return resp;
				String location = hd.getValue();
				idx = location.indexOf('?');
				if (idx > 0 && location.substring(0, idx).equals(uri)) {
					resp.setHeader("Location", net.toASCIIString() + location.substring(idx));
				} else if (location.equals(uri)) {
					resp.setHeader("Location", net.toASCIIString());
				}
				return resp;
			}
		} catch (URISyntaxException e) {
			// unrecognisable request URI
		}
		return resp;
	}

	private URI getRequestTargetWithoutQueryString(Request req)
			throws URISyntaxException {
		String path = req.getRequestLine().getUri();
		if ("*".equals(path)) {
			path = null;
		}
		if (path != null && path.indexOf('?') > 0) {
			path = path.substring(0, path.indexOf('?'));
		}
		if (path != null && !path.startsWith("/")) {
			try {
				return new URI(path);
			} catch (URISyntaxException e) {
				path = null;
			}
		}
		try {
			String host = req.getHeader("Host");
			String authority = host == null ? null : host.toLowerCase();
			return new URI("http", authority, path, null);
		} catch (URISyntaxException e) {
			// bad Host header
			return new URI(req.getRequestURL().toString());
		}
	}

}
