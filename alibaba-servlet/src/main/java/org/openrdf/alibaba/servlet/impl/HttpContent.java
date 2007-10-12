package org.openrdf.alibaba.servlet.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.openrdf.alibaba.decor.Content;

public class HttpContent implements Content {
	private HttpServletRequest req;

	public HttpContent(HttpServletRequest req) {
		this.req = req;
	}

	public String getContentType() {
		return req.getContentType();
	}

	public InputStream getInputStream() throws IOException {
		return req.getInputStream();
	}

	public Locale getLocale() {
		String lang = req.getHeader("Content-Language");
		if (lang == null)
			return null;
		String[] l = lang.split("-", 3);
		String language = l.length < 1 ? "" : l[0];
		String country = l.length < 2 ? "" : l[1];
		String variant = l.length < 3 ? "" : l[2];
		return new Locale(language, country.toUpperCase(), variant);
	}

	public BufferedReader getReader() throws IOException {
		return req.getReader();
	}
}
