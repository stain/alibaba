package org.openrdf.server.metadata.providers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.ConnectionResource;
import org.openrdf.server.metadata.ValueFactoryResource;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class RDFObjectReader implements MessageBodyReader<Object> {
	private ResourceContext ctx;
	private GraphMessageReader delegate;

	public RDFObjectReader(@Context ResourceContext ctx) {
		delegate = new GraphMessageReader(ctx);
		this.ctx = ctx;
	}

	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (ctx == null)
			return false;
		Class<GraphQueryResult> t = GraphQueryResult.class;
		if (mediaType != null && !delegate.isReadable(t, t, annotations, mediaType))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return true;
		ConnectionResource cr = ctx.getResource(ConnectionResource.class);
		if (cr == null || cr.getConnection() == null)
			return false;
		ObjectConnection con = cr.getConnection();
		return con.getObjectFactory().isNamedConcept(type);
	}

	public Object readFrom(Class<Object> type, Type genericType,
			Annotation[] annotations, MediaType media,
			MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		ObjectConnection con = getConnection();
		Resource subj = null;
		if (httpHeaders.containsKey("Content-Location")) {
			ValueFactoryResource vf = ctx.getResource(ValueFactoryResource.class);
			String location = httpHeaders.getFirst("Content-Location");
			subj = vf.createURI(location);
		}
		if (media != null) {
			try {
				Class<GraphQueryResult> t = GraphQueryResult.class;
				GraphQueryResult result = delegate.readFrom(t, t, annotations,
						media, httpHeaders, in);
				try {
					while (result.hasNext()) {
						Statement st = result.next();
						if (subj == null) {
							subj = st.getSubject();
						}
						con.add(st);
					}
				} finally {
					result.close();
				}
			} catch (QueryEvaluationException e) {
				throw new WebApplicationException(e, 400);
			} catch (RepositoryException e) {
				throw new WebApplicationException(e, 500);
			}
		}
		try {
			return con.getObject(subj);
		} catch (RepositoryException e) {
			throw new WebApplicationException(e, 500);
		}
	}

	private ObjectConnection getConnection() {
		return ctx.getResource(ConnectionResource.class).getConnection();
	}

}
