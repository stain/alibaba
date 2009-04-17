package org.openrdf.repository.sparql.query;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParser;

public class SPARQLTupleQuery extends SPARQLQuery implements TupleQuery {
	private SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser();

	public SPARQLTupleQuery(HttpClient client, String url, String query) {
		super(client, url, query);
	}

	public TupleQueryResult evaluate() throws QueryEvaluationException {
		try {
			BackgroundTupleResult result = null;
			HttpMethod response = getResponse();
			try {
				InputStream in = response.getResponseBodyAsStream();
				result = new BackgroundTupleResult(parser, in, response);
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

	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		try {
			boolean complete = false;
			HttpMethod response = getResponse();
			try {
				parser.setTupleQueryResultHandler(handler);
				parser.parse(response.getResponseBodyAsStream());
				complete = true;
			} catch (HttpException e) {
				throw new QueryEvaluationException(e);
			} catch (QueryResultParseException e) {
				throw new QueryEvaluationException(e);
			} catch (TupleQueryResultHandlerException e) {
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
		return parser.getTupleQueryResultFormat().getDefaultMIMEType();
	}
}
