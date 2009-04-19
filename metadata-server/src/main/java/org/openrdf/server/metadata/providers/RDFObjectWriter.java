package org.openrdf.server.metadata.providers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Resource;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.ConnectionResource;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class RDFObjectWriter implements MessageBodyWriter<RDFObject> {
	private static final String DESCRIBE_SELF = "CONSTRUCT {$self ?pred ?obj}\n"
			+ "WHERE {$self ?pred ?obj}";
	private ResourceContext ctx;
	private GraphMessageWriter delegate;

	public RDFObjectWriter(@Context ResourceContext ctx) {
		delegate = new GraphMessageWriter(ctx);
		this.ctx = ctx;
	}

	public long getSize(RDFObject t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (ctx == null)
			return false;
		Class<GraphQueryResult> t = GraphQueryResult.class;
		if (!delegate.isWriteable(t, t, annotations, mediaType))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return true;
		ConnectionResource cr = ctx.getResource(ConnectionResource.class);
		if (cr == null || cr.getConnection() == null)
			return false;
		ObjectConnection con = cr.getConnection();
		return con.getObjectFactory().isNamedConcept(type);
	}

	public void writeTo(RDFObject t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		ObjectConnection con = getConnection();
		Resource resource = t.getResource();
		try {
			GraphQuery query = con.prepareGraphQuery(SPARQL, DESCRIBE_SELF);
			query.setBinding("self", resource);
			delegate.writeTo(query.evaluate(), type, genericType, annotations,
					mediaType, httpHeaders, out);
		} catch (QueryEvaluationException e) {
			throw new WebApplicationException(e, 500);
		} catch (MalformedQueryException e) {
			throw new WebApplicationException(e, 500);
		} catch (RepositoryException e) {
			throw new WebApplicationException(e, 500);
		}
	}

	private ObjectConnection getConnection() {
		return ctx.getResource(ConnectionResource.class).getConnection();
	}

}
