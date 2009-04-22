package org.openrdf.server.metadata.providers;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_MAP;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openrdf.model.Value;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.ConnectionResource;

import com.sun.jersey.api.core.ResourceContext;

public class SetOfRDFObjectWriter implements MessageBodyWriter<Set<?>> {
	private static final String DESCRIBE = "CONSTRUCT {?subj ?pred ?obj}\n"
			+ "WHERE {?subj ?pred ?obj}";
	private ResourceContext ctx;
	private GraphMessageWriter delegate;

	public SetOfRDFObjectWriter(@Context ResourceContext ctx) {
		delegate = new GraphMessageWriter(ctx);
		this.ctx = ctx;
	}

	public long getSize(Set<?> t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (ctx == null)
			return false;
		Class<GraphQueryResult> g = GraphQueryResult.class;
		if (!delegate.isWriteable(g, g, annotations, mediaType))
			return false;
		if (!Set.class.isAssignableFrom(type))
			return false;
		ConnectionResource cr = ctx.getResource(ConnectionResource.class);
		if (cr == null || cr.getConnection() == null)
			return false;
		Class<?> ctype = getParameterType(genericType);
		if (Object.class.equals(ctype))
			return true;
		if (RDFObject.class.isAssignableFrom(ctype))
			return true;
		ObjectConnection con = cr.getConnection();
		return con.getObjectFactory().isNamedConcept(ctype);
	}

	public void writeTo(Set<?> set, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		GraphQueryResult result;
		try {
			if (set.isEmpty()) {
				result = new GraphQueryResultImpl(EMPTY_MAP, EMPTY_LIST
						.iterator());
			} else {
				ObjectConnection con = getConnection();
				StringBuilder qry = new StringBuilder();
				qry.append(DESCRIBE, 0, DESCRIBE.lastIndexOf('}'));
				qry.append("\nFILTER (");
				List<Value> list = new ArrayList<Value>();
				Iterator<?> iter = set.iterator();
				for (int i = 0; iter.hasNext(); i++) {
					Object obj = iter.next();
					if (obj instanceof RDFObject) {
						list.add(((RDFObject) obj).getResource());
					} else {
						list.add(con.addObject(obj));
					}
					qry.append("?subj = $_").append(i).append("||");
				}
				qry.delete(qry.length() - 2, qry.length());
				qry.append(")}");
				GraphQuery query = con
						.prepareGraphQuery(SPARQL, qry.toString());
				for (int i = 0, n = list.size(); i < n; i++) {
					query.setBinding("_" + i, list.get(i));
				}
				result = query.evaluate();
			}
		} catch (QueryEvaluationException e) {
			throw new WebApplicationException(e, 500);
		} catch (MalformedQueryException e) {
			throw new WebApplicationException(e, 500);
		} catch (RepositoryException e) {
			throw new WebApplicationException(e, 500);
		}
		delegate.writeTo(result, type, genericType, annotations, mediaType,
				httpHeaders, out);
	}

	private ObjectConnection getConnection() {
		return ctx.getResource(ConnectionResource.class).getConnection();
	}

	private Class<?> getParameterType(Type genericType) {
		if (genericType instanceof Class)
			return Object.class;
		if (!(genericType instanceof ParameterizedType))
			return Object.class;
		ParameterizedType ptype = (ParameterizedType) genericType;
		Type[] atypes = ptype.getActualTypeArguments();
		if (atypes.length != 1)
			return Object.class;
		Type t = atypes[0];
		if (t instanceof Class)
			return (Class<?>) t;
		return Object.class;
	}

}
