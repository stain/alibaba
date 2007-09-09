package org.openrdf.alibaba.servlet.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.alibaba.decor.UrlResolver;
import org.openrdf.alibaba.servlet.Response;

public class HttpResponse implements Response {
	private HttpServletRequest req;

	private HttpServletResponse resp;

	private UrlResolver urlResolver;

	public HttpResponse(HttpServletRequest req, HttpServletResponse resp) {
		super();
		this.req = req;
		this.resp = resp;
	}

	public String[] getAcceptedTypes() {
		String accept = req.getParameter("accept");
		if (accept == null)
			accept = req.getHeader("Accept");
		if (accept == null)
			return null;
		return accept.split(",");
	}

	public Locale[] getAcceptedLocales() {
		List<Locale> result = new ArrayList<Locale>();
		Enumeration<Locale> locales = req.getLocales();
		while (locales.hasMoreElements()) {
			result.add(locales.nextElement());
		}
		return result.toArray(new Locale[result.size()]);
	}

	public Locale getLocale() {
		return req.getLocale();
	}

	public OutputStream getOutputStream() throws IOException {
		return resp.getOutputStream();
	}

	public PrintWriter getWriter() throws IOException {
		return resp.getWriter();
	}

	public void setContentType(String contentType) {
		resp.setContentType(contentType);
	}

	public void setLocale(Locale locale) {
		resp.setLocale(locale);
	}

	public UrlResolver getUrlResolver() {
		return urlResolver;
	}

	public void setUrlResolver(UrlResolver urlResolver) {
		this.urlResolver = urlResolver;
	}
}
