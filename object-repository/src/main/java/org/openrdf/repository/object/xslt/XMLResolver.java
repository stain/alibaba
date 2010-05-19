package org.openrdf.repository.object.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class XMLResolver implements URIResolver {
	private static final String ACCEPT_XML = "application/xml, application/xslt+xml, text/xml, text/xsl";

	public Source resolve(String href, String base) throws TransformerException {
		try {
			java.net.URL url = new java.net.URL(href);
			URLConnection con = url.openConnection();
			con.addRequestProperty("Accept", ACCEPT_XML);
			con.addRequestProperty("Accept-Encoding", "gzip");
			String encoding = con.getHeaderField("Content-Encoding");
			InputStream in = con.getInputStream();
			if (encoding != null && encoding.contains("gzip")) {
				in = new GZIPInputStream(in);
			}
			return new StreamSource(in, con.getURL().toExternalForm());
		} catch (MalformedURLException e) {
			throw new TransformerException(e);
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

}
