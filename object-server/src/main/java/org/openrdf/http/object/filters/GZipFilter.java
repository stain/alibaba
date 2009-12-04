package org.openrdf.http.object.filters;

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
		HttpServletResponse resp = (HttpServletResponse) response;
		String method = req.getMethod();
		if (method.equals("GET") || method.equals("PROFIND")) {
			GZipResponse gzip = new GZipResponse(resp, false);
			chain.doFilter(req, gzip);
			gzip.flush();
		} else if (method.equals("HEAD")) {
			GZipResponse gzip = new GZipResponse(resp, true);
			chain.doFilter(req, gzip);
			gzip.flush();
		} else {
			chain.doFilter(req, resp);
		}
	}
}
