package org.openrdf.http.object.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ContentMD5Filter implements Filter {

	public void init(FilterConfig arg0) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		String md5 = req.getHeader("Content-MD5");
		if (md5 == null) {
			chain.doFilter(new ContentMD5Request(req), response);
		} else {
			chain.doFilter(req, response);
		}
	}

}
