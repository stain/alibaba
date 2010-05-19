package org.openrdf.repository.object.xslt;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;


public class CachedTransformerFactory extends TransformerFactory {
	private static final String ACCEPT_XSLT = "application/xslt+xml, text/xsl, application/xml;q=0.2, text/xml;q=0.2";
	private static final Pattern SMAXAGE = Pattern
			.compile("s-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("max-age\\s*=\\s*(\\d+)");
	private TransformerFactory delegate;
	private XMLResolver resolver;
	private String uri;
	private String tag;
	private Integer maxage;
	private long expires;
	private Templates xslt;

	public CachedTransformerFactory() {
		this(TransformerFactory.newInstance());
	}

	public CachedTransformerFactory(TransformerFactory delegate) {
		this.delegate = delegate;
		this.resolver = new XMLResolver();
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
		} else if (expires == 0 || expires > currentTimeMillis()) {
			return xslt;
		}
		URLConnection con = new URL(systemId).openConnection();
		con.addRequestProperty("Accept", ACCEPT_XSLT);
		con.addRequestProperty("Accept-Encoding", "gzip");
		if (tag != null && xslt != null) {
			con.addRequestProperty("If-None-Match", tag);
		}
		if (isStorable(con.getHeaderField("Cache-Control"))) {
			xslt = newTemplates(con);
			return xslt;
		} else {
			xslt = null;
			tag = null;
			expires = 0;
			maxage = 0;
			return newTemplates(con);
		}
	}

	private boolean isStorable(String cc) {
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private Templates newTemplates(URLConnection con) throws IOException,
			TransformerException {
		String cacheControl = con.getHeaderField("Cache-Control");
		long date = con.getHeaderFieldDate("Expires", expires);
		expires = getExpires(cacheControl, date);
		if (con instanceof HttpURLConnection) {
			int status = ((HttpURLConnection) con).getResponseCode();
			if (status == 304 || status == 412) {
				assert xslt != null;
				return xslt; // Not Modified
			}
		}
		tag = con.getHeaderField("ETag");
		String encoding = con.getHeaderField("Content-Encoding");
		InputStream in = con.getInputStream();
		if (encoding != null && encoding.contains("gzip")) {
			in = new GZIPInputStream(in);
		}
		String base = con.getURL().toExternalForm();
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
