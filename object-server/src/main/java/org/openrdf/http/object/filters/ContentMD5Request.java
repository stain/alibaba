package org.openrdf.http.object.filters;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentMD5Request extends HttpServletRequestWrapper implements
		HttpServletRequest {
	private Logger logger = LoggerFactory.getLogger(ContentMD5Request.class);
	private boolean closed;
	private byte[] body;
	private IOException error;
	private String md5;

	public ContentMD5Request(HttpServletRequest request) {
		super(request);
	}

	@Override
	public String getHeader(String name) {
		if ("Content-MD5".equalsIgnoreCase(name))
			return getContentMD5();
		return super.getHeader(name);
	}

	@Override
	public Enumeration getHeaders(String name) {
		if ("Content-MD5".equalsIgnoreCase(name)) {
			Vector<String> result = new Vector<String>();
			result.add(getContentMD5());
			return result.elements();
		}
		return super.getHeaders(name);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		closed = true;
		if (error != null)
			throw error;
		if (body == null)
			return super.getInputStream();
		return new InputServletStream(new ByteArrayInputStream(body));
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException();
	}

	private String getContentMD5() {
		if (md5 != null)
			return md5;
		if (closed)
			return super.getHeader("Content-MD5");
		try {
			ServletInputStream in = getInputStream();
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ContentMD5Stream out = new ContentMD5Stream(baos);
				try {
					int read;
					byte[] buf = new byte[1024];
					while ((read = in.read(buf)) >= 0) {
						if (read > 0) {
							out.write(buf, 0, read);
						}
					}
				} finally {
					out.close();
				}
				body = baos.toByteArray();
				return md5 = out.getContentMD5();
			} finally {
				in.close();
			}
		} catch (NoSuchAlgorithmException e) {
			logger.warn(e.toString(), e);
			return super.getHeader("Content-MD5");
		} catch (IOException e) {
			this.error = e;
			return super.getHeader("Content-MD5");
		}
	}
}
