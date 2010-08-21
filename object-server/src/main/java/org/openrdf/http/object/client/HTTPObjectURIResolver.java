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
import org.openrdf.http.object.exceptions.ResponseException;

/**
 * Resolves XML files using the {@link HTTPObjectClient}.
 * 
 * @author James Leigh
 * 
 */
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
		if (code >= 300 && code < 400 && resp.containsHeader("Location")) {
			if (entity != null) {
				entity.consumeContent();
			}
			if (max < 0)
				throw new BadGateway("To Many Redirects: " + url);
			Header location = resp.getFirstHeader("Location");
			return getXML(client, location.getValue(), max - 1);
		} else if (code >= 300) {
			throw ResponseException.create(resp);
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
