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
import java.net.URLDecoder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicRequestLine;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Request;

/**
 * Extracts a percent encoded URI from a wrapping URL.
 * 
 * @author James Leigh
 * 
 */
public class IdentityPrefix extends Filter {
	private String prefix;

	public IdentityPrefix(Filter delegate) {
		super(delegate);
	}

	public String getIdentityPrefix() {
		return prefix;
	}

	public void setIdentityPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Request filter(Request req) throws IOException {
		if (prefix == null)
			return super.filter(req);
		String uri = req.getURI();
		if (uri != null && uri.startsWith(prefix)) {
			String encoded = uri.substring(prefix.length());
			String target = URLDecoder.decode(encoded, "UTF-8");
			RequestLine line = req.getRequestLine();
			int idx = line.getUri().indexOf('?');
			if (idx > 0) {
				target = target + line.getUri().substring(idx);
			}
			String method = line.getMethod();
			ProtocolVersion version = line.getProtocolVersion();
			line = new BasicRequestLine(method, target, version);
			req.setRequestLine(line);
		}
		return super.filter(req);
	}

	public HttpResponse filter(Request req, HttpResponse resp)
			throws IOException {
		resp = super.filter(req, resp);
		if (prefix == null)
			return resp;
		String line = req.getRequestLine().getUri();
		String orig = req.getOriginalRequestLine().getUri();
		if (line.equals(orig))
			return resp;
		String uri = req.getURIFromRequestTarget(orig);
		if (uri != null && uri.startsWith(prefix)) {
			String target = req.getURI();
			Header[] headers = resp.getHeaders("Location");
			resp.removeHeaders("Location");
			for (Header hd : headers) {
				String location = hd.getValue();
				int idx = location.indexOf('?');
				if (idx > 0 && location.substring(0, idx).equals(target)) {
					resp.addHeader("Location", uri + location.substring(idx));
				} else if (location.equals(target)) {
					resp.addHeader("Location", uri);
				} else {
					resp.addHeader(hd);
				}
			}
		}
		return resp;
	}

}
