package org.openrdf.repository.sparql.query;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLParser;

public class SPARQLBooleanQuery extends SPARQLQuery implements BooleanQuery {
	private SPARQLBooleanXMLParser parser = new SPARQLBooleanXMLParser();

	public SPARQLBooleanQuery(HttpClient client, String url, String query) {
		super(client, url, query);
	}

	public boolean evaluate() throws QueryEvaluationException {
		try {
			boolean complete = false;
			HttpMethod response = getResponse();
			try {
				boolean result = parser.parse(response.getResponseBodyAsStream());
				complete = true;
				return result;
			} catch (HttpException e) {
				throw new QueryEvaluationException(e);
			} catch (QueryResultParseException e) {
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
		return parser.getBooleanQueryResultFormat().getDefaultMIMEType();
	}
}
