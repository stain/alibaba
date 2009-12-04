package org.openrdf.http.object.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MD5ValidationRequest extends HttpServletRequestWrapper implements
		HttpServletRequest {
	private HttpServletRequest req;

	public MD5ValidationRequest(HttpServletRequest request) {
		super(request);
		this.req = request;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		String md5 = req.getHeader("Content-MD5");
		ServletInputStream in = super.getInputStream();
		if (md5 == null)
			return in;
		try {
			return new InputServletStream(new MD5ValidatingStream(in, md5));
		} catch (NoSuchAlgorithmException e) {
			Logger logger = LoggerFactory.getLogger(MD5ValidationRequest.class);
			logger.warn(e.getMessage(), e);
			return in;
		}
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException();
	}

}
