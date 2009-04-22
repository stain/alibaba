package org.openrdf.server.metadata.providers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.ConnectionResource;
import org.openrdf.server.metadata.ValueFactoryResource;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class SetOfRDFObjectReader implements MessageBodyReader<Set<?>> {
	private ResourceContext ctx;
	private GraphMessageReader delegate;

	public SetOfRDFObjectReader(@Context ResourceContext ctx) {
		delegate = new GraphMessageReader(ctx);
		this.ctx = ctx;
	}

	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (ctx == null)
			return false;
		Class<GraphQueryResult> g = GraphQueryResult.class;
		if (mediaType != null
				&& !delegate.isReadable(g, g, annotations, mediaType))
			return false;
		if (!Set.class.equals(type))
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

	public Set<?> readFrom(Class<Set<?>> type, Type genericType,
			Annotation[] annotations, MediaType media,
			MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		try {
			ObjectConnection con = getConnection();
			Set<Resource> subjects = new HashSet<Resource>();
			Set<Value> objects = new HashSet<Value>();
			if (media == null && httpHeaders.containsKey("Content-Location")) {
				ValueFactoryResource vf;
				vf = ctx.getResource(ValueFactoryResource.class);
				String location = httpHeaders.getFirst("Content-Location");
				subjects.add(vf.createURI(location));
			} else if (media != null) {
				Class<GraphQueryResult> t = GraphQueryResult.class;
				GraphQueryResult result = delegate.readFrom(t, t, annotations,
						media, httpHeaders, in);
				try {
					while (result.hasNext()) {
						Statement st = result.next();
						subjects.add(st.getSubject());
						Value obj = st.getObject();
						if (obj instanceof Resource && !(obj instanceof URI)) {
							objects.add(obj);
						}
						con.add(st);
					}
				} finally {
					result.close();
				}
			}
			subjects.removeAll(objects);
			Resource[] resources = new Resource[subjects.size()];
			Class<?> ctype = getParameterType(genericType);
			return con.getObjects(ctype, subjects.toArray(resources)).asSet();
		} catch (QueryEvaluationException e) {
			throw new WebApplicationException(e, 400);
		} catch (RepositoryException e) {
			throw new WebApplicationException(e, 500);
		}
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
