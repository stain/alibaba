package org.openrdf.server.metadata.filters;

import java.io.IOException;

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
		String ae = req.getHeader("Accept-Encoding");
		if (ae == null || ae.indexOf("gzip") == -1) {
			chain.doFilter(req, res);
		} else {
			GZipResponseWrapper gzip = new GZipResponseWrapper(res);
			chain.doFilter(req, gzip);
			gzip.flush();
			return;
		}
	}
}
