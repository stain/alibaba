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

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.repository.object.xslt.ErrorCatcher;

public class HTTPObjectTransformerFactory extends TransformerFactory {
	private static final String ACCEPT_XSLT = "application/xslt+xml, text/xsl, application/xml;q=0.2, text/xml;q=0.2";
	private static final Pattern SMAXAGE = Pattern
			.compile("s-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("max-age\\s*=\\s*(\\d+)");
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
		if (uri == null || !uri.equals(systemId)) {
			uri = systemId;
			xslt = null;
			tag = null;
			expires = 0;
			maxage = null;
		} else if (xslt != null && (expires == 0 || expires > currentTimeMillis())) {
			return xslt;
		}
		HttpRequest con = new BasicHttpRequest("GET", systemId);
		con.setHeader("Accept", ACCEPT_XSLT);
		if (tag != null && xslt != null) {
			con.setHeader("If-None-Match", tag);
		}
		HttpResponse resp = HTTPObjectClient.getInstance().service(con);
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
		String cacheControl = getHeader(con, "Cache-Control");
		expires = getExpires(cacheControl, expires);
		int status = con.getStatusLine().getStatusCode();
		if (status == 304 || status == 412) {
			assert xslt != null;
			return xslt; // Not Modified
		}
		tag = getHeader(con, "ETag");
		InputStream in = con.getEntity().getContent();
		ErrorCatcher error = new ErrorCatcher(base);
		delegate.setErrorListener(error);
		try {
			Source source = new StreamSource(in, base);
			return delegate.newTemplates(source);
		} finally {
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
