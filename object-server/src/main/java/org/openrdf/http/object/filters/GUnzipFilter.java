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
package org.openrdf.http.object.filters;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Uncompresses the response if the requesting client does not explicitly say it accepts gzip.
 */
public class GUnzipFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		Boolean gzip = null;
		boolean encode = false; // gunzip by default
		Enumeration<String> ae = req.getHeaders("Accept-Encoding");
		while (ae.hasMoreElements()) {
			for (String value : ae.nextElement().split("\\s*,\\s*")) {
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
		if ("gzip".equals(req.getHeader("Content-Encoding"))) {
			req = new GUnzipRequest(req);
		} else if (req.getHeader("Content-Encoding") != null
				&& !"identity".equals(req.getHeader("Content-Encoding"))) {
			res.sendError(415); // Unsupported Media Type
		}
		if (gzip == null ? encode : gzip) {
			chain.doFilter(req, res);
		} else {
			GUnzipResponse gunzip = new GUnzipResponse(res, "HEAD".equals(req.getMethod()));
			chain.doFilter(req, gunzip);
			gunzip.flush();
		}
	}
}
