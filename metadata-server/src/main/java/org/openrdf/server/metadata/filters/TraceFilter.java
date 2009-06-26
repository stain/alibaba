package org.openrdf.server.metadata.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TraceFilter implements Filter {

	public void init(FilterConfig arg0) throws ServletException {
		// no-op
	}

	public void destroy() {
		// no-op
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		if ("TRACE".equals(req.getMethod())) {
			resp.setStatus(200);
			resp.setDateHeader("Date", System.currentTimeMillis());
			resp.setContentType("message/http");
			PrintWriter out = resp.getWriter();
			out.append("TRACE ").append(req.getRequestURI()).append("\r\n");
			Enumeration headers = req.getHeaderNames();
			while (headers.hasMoreElements()) {
				String header = (String) headers.nextElement();
				Enumeration values = req.getHeaders(header);
				while (values.hasMoreElements()) {
					String value = (String) values.nextElement();
					out.append(header).append(": ");
					out.append(value).append("\r\n");
				}
			}
			out.append("\r\n");
		} else if ("OPTIONS".equals(req.getMethod()) && "*".equals(req.getRequestURI())) {
			resp.setStatus(204);
			resp.setDateHeader("Date", System.currentTimeMillis());
			resp.setHeader("Allow", "OPTIONS, TRACE, GET, HEAD, PUT, DELETE");
		} else {
			chain.doFilter(request, response);
		}
	}

}
