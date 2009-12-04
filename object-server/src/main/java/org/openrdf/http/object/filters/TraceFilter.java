package org.openrdf.http.object.filters;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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
	        int responseLength;
	        String CRLF = "\r\n";
	        String responseString = "TRACE " + req.getRequestURI() +
	                " " + req.getProtocol();

	        Enumeration reqHeaderEnum = req.getHeaderNames();
	        while (reqHeaderEnum.hasMoreElements()) {
	            String headerName = (String) reqHeaderEnum.nextElement();
	            responseString += CRLF + headerName + ": " +
	                    req.getHeader(headerName);
	        }

	        responseString += CRLF;
	        responseLength = responseString.length();
	        resp.setContentType("message/http");
	        resp.setContentLength(responseLength);
	        ServletOutputStream out = resp.getOutputStream();
	        try {
	        	out.print(responseString);
	        } finally {
	        	out.close();
	        }
		} else if ("OPTIONS".equals(req.getMethod()) && "*".equals(req.getRequestURI())) {
			resp.setStatus(204);
			resp.setDateHeader("Date", System.currentTimeMillis());
			resp.setHeader("Allow", "OPTIONS, TRACE, GET, HEAD, PUT, DELETE");
		} else {
			chain.doFilter(request, response);
		}
	}

}
