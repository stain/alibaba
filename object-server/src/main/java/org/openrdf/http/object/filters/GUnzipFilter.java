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
package org.openrdf.http.object.filters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Request;

/**
 * Uncompresses the response if the requesting client does not explicitly say it
 * accepts gzip.
 */
public class GUnzipFilter extends Filter {
	private static String hostname;
	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "AliBaba";
		}
	}
	private static String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";

	public GUnzipFilter(Filter delegate) {
		super(delegate);
	}

	public Request filter(Request req) throws IOException {
		if ("gzip".equals(req.getHeader("Content-Encoding"))) {
			req.setHeader("Content-Encoding", "identity");
			req.setEntity(new GUnzipEntity(req.getEntity()));
		}
		return super.filter(req);
	}

	public HttpResponse filter(Request req, HttpResponse resp) throws IOException {
		resp = super.filter(req, resp);
		Boolean gzip = null;
		boolean encode = false; // gunzip by default
		for (Header header : req.getHeaders("Accept-Encoding")) {
			for (String value : header.getValue().split("\\s*,\\s*")) {
				String[] items = value.split("\\s*;\\s*");
				int q = 1;
				for (int i = 1; i < items.length; i++) {
					if (items[i].startsWith("q=")) {
						q = Integer.parseInt(items[i].substring(2));
					}
				}
				if ("gzip".equals(items[0])) {
					gzip = q > 0;
				} else if ("*".equals(items[0])) {
					encode = q > 0;
				}
			}
		}
		if (gzip == null ? encode : gzip)
			return resp;
		Header encoding = resp.getFirstHeader("Content-Encoding");
		if (encoding != null && "gzip".equals(encoding.getValue())) {
			resp.removeHeaders("ETag");
			resp.removeHeaders("Content-MD5");
			resp.removeHeaders("Content-Length");
			resp.removeHeaders("Content-Encoding");
			resp.addHeader("Transfer-Encoding", "chunked");
			resp.addHeader("Warning", WARN_214);
			resp.setEntity(new GUnzipEntity(resp.getEntity()));
		}
		return resp;
	}
}
