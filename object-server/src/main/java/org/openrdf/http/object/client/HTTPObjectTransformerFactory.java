/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.client;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.exceptions.BadGateway;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.repository.object.xslt.CachedTransformerFactory;
import org.openrdf.repository.object.xslt.ErrorCatcher;

/**
 * A variation of the {@link CachedTransformerFactory} that uses
 * {@link HTTPObjectClient}.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectTransformerFactory extends TransformerFactory {
	public static void resetTransforms() {
		resetCount++;
	}

	public static void invalidateTransforms() {
		invalidateCount++;
	}

	private static final String ACCEPT_XSLT = "application/xslt+xml, text/xsl, application/xml;q=0.2, text/xml;q=0.2";
	private static final Pattern SMAXAGE = Pattern
			.compile("s-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("max-age\\s*=\\s*(\\d+)");
	private static volatile int invalidateCount;
	private static volatile int resetCount;
	private int invalidateLastCount = invalidateCount;
	private int resetLastCount = resetCount;
	private TransformerFactory delegate;
	private HTTPObjectURIResolver resolver;
	private String uri;
	private String tag;
	private Integer maxage;
	private long expires;
	private Templates xslt;

	public HTTPObjectTransformerFactory() {
		this(TransformerFactory.newInstance());
	}

	public HTTPObjectTransformerFactory(TransformerFactory delegate) {
		this.delegate = delegate;
		this.resolver = new HTTPObjectURIResolver();
	}

	public Source getAssociatedStylesheet(Source source, String media,
			String title, String charset)
			throws TransformerConfigurationException {
		return delegate.getAssociatedStylesheet(source, media, title, charset);
	}

	public Object getAttribute(String name) {
		return delegate.getAttribute(name);
	}

	public ErrorListener getErrorListener() {
		return delegate.getErrorListener();
	}

	public boolean getFeature(String name) {
		return delegate.getFeature(name);
	}

	public Transformer newTransformer()
			throws TransformerConfigurationException {
		return delegate.newTransformer();
	}

	public Transformer newTransformer(Source source)
			throws TransformerConfigurationException {
		return delegate.newTransformer(source);
	}

	public void setAttribute(String name, Object value) {
		delegate.setAttribute(name, value);
	}

	public void setErrorListener(ErrorListener listener) {
		delegate.setErrorListener(listener);
	}

	public void setFeature(String name, boolean value)
			throws TransformerConfigurationException {
		delegate.setFeature(name, value);
	}

	public String toString() {
		return delegate.toString();
	}

	public URIResolver getURIResolver() {
		if (resolver != null)
			return resolver;
		return delegate.getURIResolver();
	}

	public void setURIResolver(URIResolver resolver) {
		this.resolver = null;
		delegate.setURIResolver(resolver);
	}

	public Templates newTemplates(Source source)
			throws TransformerConfigurationException {
		if (source instanceof StreamSource) {
			StreamSource ss = (StreamSource) source;
			if (ss.getInputStream() == null && ss.getReader() == null
					&& ss.getSystemId() != null) {
				try {
					return newTemplates(ss.getSystemId());
				} catch (TransformerException e) {
					throw new TransformerConfigurationException(e);
				} catch (IOException e) {
					throw new TransformerConfigurationException(e);
				}
			}
		}
		return delegate.newTemplates(source);
	}

	private synchronized Templates newTemplates(String systemId)
			throws TransformerException, IOException {
		if (uri == null || !uri.equals(systemId)
				|| resetLastCount != resetCount) {
			uri = systemId;
			xslt = null;
			tag = null;
			expires = 0;
			maxage = null;
			resetLastCount = resetCount;
		} else if (xslt != null
				&& (expires == 0 || expires > currentTimeMillis())
				&& invalidateLastCount == invalidateCount) {
			return xslt;
		}
		invalidateLastCount = invalidateCount;
		HttpRequest con = new BasicHttpRequest("GET", systemId);
		con.setHeader("Accept", ACCEPT_XSLT);
		if (tag != null && xslt != null) {
			con.setHeader("If-None-Match", tag);
		}
		HttpResponse resp = getXSL(systemId, 20);
		if (isStorable(getHeader(resp, "Cache-Control"))) {
			return xslt = newTemplates(systemId, resp);
		} else {
			xslt = null;
			tag = null;
			expires = 0;
			maxage = null;
			return newTemplates(systemId, resp);
		}
	}

	private HttpResponse getXSL(String url, int max)
			throws IOException {
		HttpRequest req = new BasicHttpRequest("GET", url);
		req.setHeader("Accept", ACCEPT_XSLT);
		if (tag != null && xslt != null) {
			req.setHeader("If-None-Match", tag);
		}
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.service(req);
		int code = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (code >= 300 && code < 400 && code != 304) {
			if (entity != null) {
				entity.consumeContent();
			}
			if (max < 0)
				throw new BadGateway("To Many Redirects: " + url);
			Header location = resp.getFirstHeader("Location");
			if (location == null)
				return resp;
			return getXSL(location.getValue(), max - 1);
		}
		return resp;
	}

	private String getHeader(HttpResponse resp, String name) {
		if (resp.containsHeader(name))
			return resp.getFirstHeader(name).getValue();
		return null;
	}

	private boolean isStorable(String cc) {
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private Templates newTemplates(String base, HttpResponse con)
			throws IOException, TransformerException {
		HttpEntity entity = con.getEntity();
		ErrorCatcher error = new ErrorCatcher(base);
		InputStream in = entity == null ? null : entity.getContent();
		try {
			String cacheControl = getHeader(con, "Cache-Control");
			expires = getExpires(cacheControl, expires);
			int status = con.getStatusLine().getStatusCode();
			if (status == 304 || status == 412) {
				assert xslt != null;
				return xslt; // Not Modified
			} else if (status >= 300) {
				throw ResponseException.create(con);
			}
			tag = getHeader(con, "ETag");
			delegate.setErrorListener(error);
			Source source = new StreamSource(in, base);
			return delegate.newTemplates(source);
		} finally {
			if (in != null) {
				in.close();
			}
			if (error.isFatal())
				throw error.getFatalError();
		}
	}

	private long getExpires(String cacheControl, long defaultValue) {
		if (cacheControl != null && cacheControl.contains("s-maxage")) {
			try {
				Matcher m = SMAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		} else if (cacheControl != null && cacheControl.contains("max-age")) {
			try {
				Matcher m = MAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		if (maxage != null)
			return currentTimeMillis() + maxage * 1000;
		return defaultValue;
	}

}
