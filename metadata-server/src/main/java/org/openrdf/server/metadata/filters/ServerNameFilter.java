package org.openrdf.server.metadata.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ServerNameFilter implements Filter {
	private String name;

	public ServerNameFilter(String name) {
		this.name = name;
	}

	public String getServerName() {
		return name;
	}

	public void setServerName(String name) {
		this.name = name;
	}

	public void init(FilterConfig arg0) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		if (name != null) {
			((HttpServletResponse)resp).setHeader("Server", name);
		}
		chain.doFilter(req, resp);
	}

}
