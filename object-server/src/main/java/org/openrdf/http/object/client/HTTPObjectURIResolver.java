package org.openrdf.http.object.client;

import info.aduna.net.ParsedURI;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.openrdf.http.object.exceptions.BadGateway;

public class HTTPObjectURIResolver implements URIResolver {
	private static final String ACCEPT_XML = "application/xml, application/xslt+xml, text/xml, text/xsl";

	public Source resolve(String href, String base) throws TransformerException {
		try {
			HTTPObjectClient client = HTTPObjectClient.getInstance();
			return getXML(client, resolveURI(href, base), 20);
		} catch (IOException e) {
			throw new TransformerException(e);
		}
	}

	private Source getXML(HTTPObjectClient client, String url, int max)
			throws IOException {
		HttpRequest req = new BasicHttpRequest("GET", url);
		req.setHeader("Accept", ACCEPT_XML);
		HttpResponse resp = client.service(req);
		int code = resp.getStatusLine().getStatusCode();
		HttpEntity entity = resp.getEntity();
		if (code >= 300 && code < 400) {
			if (entity != null) {
				entity.consumeContent();
			}
			if (max < 0)
				throw new BadGateway("To Many Redirects: " + url);
			Header location = resp.getFirstHeader("Location");
			return getXML(client, location.getValue(), max - 1);
		}
		return new StreamSource(entity.getContent(), url);
	}

	private String resolveURI(String href, String base) {
		if (href != null && href.contains(":"))
			return href;
		ParsedURI abs = new ParsedURI(base);
		if (href != null) {
			abs = abs.resolve(href);
		}
		return abs.toString();
	}

}
