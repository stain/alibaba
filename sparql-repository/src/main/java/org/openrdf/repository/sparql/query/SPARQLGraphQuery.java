/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.sparql.query;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.resultio.UnsupportedQueryResultFormatException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;

public class SPARQLGraphQuery extends SPARQLQuery implements GraphQuery {
	private RDFParserRegistry registry = RDFParserRegistry.getInstance();

	public SPARQLGraphQuery(HttpClient client, String url, String query) {
		super(client, url, query);
	}

	public GraphQueryResult evaluate() throws QueryEvaluationException {
		try {
			BackgroundGraphResult result = null;
			HttpMethodBase response = getResponse();
			try {
				RDFParser parser = getParser(response);
				InputStream in = response.getResponseBodyAsStream();
				String charset_str = response.getResponseCharSet();
				Charset charset;
				try {
					charset = Charset.forName(charset_str);
				} catch (IllegalCharsetNameException e) {
					// work around for Joseki-3.2
					// Content-Type: application/rdf+xml; charset=application/rdf+xml
					charset = Charset.forName("UTF-8");
				}
				result = new BackgroundGraphResult(parser, in, charset,
						getUrl(), response);
				execute(result);
				return result;
			} catch (HttpException e) {
				throw new QueryEvaluationException(e);
			} finally {
				if (result == null) {
					response.abort();
				}
			}
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public void evaluate(RDFHandler handler) throws QueryEvaluationException,
			RDFHandlerException {
		boolean complete = false;
		try {
			HttpMethod response = getResponse();
			try {
				RDFParser parser = getParser(response);
				parser.setRDFHandler(handler);
				parser.parse(response.getResponseBodyAsStream(), getUrl());
				complete = true;
			} catch (HttpException e) {
				throw new QueryEvaluationException(e);
			} catch (RDFParseException e) {
				throw new QueryEvaluationException(e);
			} catch (RDFHandlerException e) {
				throw new QueryEvaluationException(e);
			} finally {
				if (!complete) {
					response.abort();
				}
			}
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	protected String getAccept() {
		StringBuilder sb = new StringBuilder();
		Set<RDFFormat> rdfFormats = registry.getKeys();
		for (RDFFormat format : rdfFormats) {
			for (String mimeType : format.getMIMETypes()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(mimeType);
			}
		}
		return sb.toString();
	}

	private RDFParser getParser(HttpMethod response) {
		for (Header header : response.getResponseHeaders("Content-Type")) {
			for (HeaderElement headerEl : header.getElements()) {
				String mimeType = headerEl.getName();
				if (mimeType != null) {
					RDFFormat format = registry
							.getFileFormatForMIMEType(mimeType);
					RDFParserFactory factory = registry.get(format);
					if (factory != null)
						return factory.getParser();
				}
			}
		}
		throw new UnsupportedQueryResultFormatException(
				"No parser factory available for this graph query result format");
	}
}
