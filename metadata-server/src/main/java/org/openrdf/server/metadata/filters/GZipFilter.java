package org.openrdf.server.metadata.filters;

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

public class GZipFilter implements Filter {

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
		boolean encodable = false;
		boolean transformable = true;
		Enumeration ae = req.getHeaders("Accept-Encoding");
		while (ae.hasMoreElements()) {
			if (ae.nextElement().toString().contains("gzip")) {
				encodable = true;
			}
		}
		Enumeration cc = req.getHeaders("Cache-Control");
		while (cc.hasMoreElements()) {
			if (cc.nextElement().toString().contains("no-transform")) {
				transformable = false;
			}
		}
		if (encodable && transformable) {
			GZipResponseWrapper gzip = new GZipResponseWrapper(res);
			chain.doFilter(req, gzip);
			gzip.flush();
		} else {
			chain.doFilter(req, res);
		}
	}
}
