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
