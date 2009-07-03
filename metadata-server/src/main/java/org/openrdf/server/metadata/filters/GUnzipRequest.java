package org.openrdf.server.metadata.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class GUnzipRequest extends HttpServletRequestWrapper implements
		HttpServletRequest {

	public GUnzipRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		GZIPInputStream gunzip = new GZIPInputStream(super.getInputStream());
		return new InputServletStream(gunzip);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException();
	}

}
