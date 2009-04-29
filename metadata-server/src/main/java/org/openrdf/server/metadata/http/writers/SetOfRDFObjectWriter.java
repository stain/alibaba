package org.openrdf.server.metadata.http.writers;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.openrdf.model.Model;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;

public class SetOfRDFObjectWriter implements MessageBodyWriter<Set<?>> {
	private static final String DESCRIBE = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";
	private GraphMessageWriter delegate;

	public SetOfRDFObjectWriter() {
		delegate = new GraphMessageWriter();
	}

	public long getSize(Set<?> t, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, MediaType mediaType) {
		Class<GraphQueryResult> g = GraphQueryResult.class;
		if (!delegate.isWriteable(g, mediaType))
			return false;
		if (Model.class.isAssignableFrom(type))
			return false;
		return Set.class.isAssignableFrom(type);
	}

	public String getContentType(Class<?> type, MediaType mediaType) {
		return delegate.getContentType(null, mediaType);
	}

	public void writeTo(Set<?> set, String base, MediaType mediaType,
			OutputStream out) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException {
		GraphQueryResult result = getGraphResult(set);
		delegate.writeTo(result, base, mediaType, out);
	}

	private GraphQueryResult getGraphResult(Set<?> set)
			throws RepositoryException, QueryEvaluationException {
		GraphQueryResult result;
		if (set.isEmpty()) {
			result = new GraphQueryResultImpl(EMPTY_MAP, EMPTY_LIST.iterator());
		} else {
			ObjectConnection con = null;
			StringBuilder qry = new StringBuilder();
			qry.append(DESCRIBE, 0, DESCRIBE.lastIndexOf('}'));
			qry.append("\nFILTER (");
			List<Value> list = new ArrayList<Value>();
			Iterator<?> iter = set.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				Object obj = iter.next();
				if (con == null) {
					con = ((RDFObject) obj).getObjectConnection();
				}
				list.add(((RDFObject) obj).getResource());
				qry.append("?subj = $_").append(i).append("||");
			}
			qry.delete(qry.length() - 2, qry.length());
			qry.append(")}");
			try {
				GraphQuery query = con
						.prepareGraphQuery(SPARQL, qry.toString());
				for (int i = 0, n = list.size(); i < n; i++) {
					query.setBinding("_" + i, list.get(i));
				}
				result = query.evaluate();
			} catch (MalformedQueryException e) {
				throw new AssertionError(e);
			}

		}
		return result;
	}

}
