package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

@Provider
public class SetOfRDFObjectReader implements MessageBodyReader<Set<?>> {
	private GraphMessageReader delegate;

	public SetOfRDFObjectReader() {
		delegate = new GraphMessageReader();
	}

	public boolean isReadable(Class<?> type, Type genericType,
			MediaType mediaType, ObjectConnection con) {
		Class<GraphQueryResult> g = GraphQueryResult.class;
		if (mediaType != null && !delegate.isReadable(g, g, mediaType, con))
			return false;
		if (!Set.class.equals(type))
			return false;
		Class<?> ctype = getParameterType(genericType);
		if (Object.class.equals(ctype))
			return true;
		if (RDFObject.class.isAssignableFrom(ctype))
			return true;
		return con.getObjectFactory().isNamedConcept(ctype);
	}

	protected Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

	public Set<?> readFrom(Class<? extends Set<?>> type, Type genericType,
			MediaType media, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException {
		Set<Resource> subjects = new HashSet<Resource>();
		Set<Value> objects = new HashSet<Value>();
		if (media == null && location != null) {
			ValueFactory vf = con.getValueFactory();
			subjects.add(vf.createURI(location));
		} else if (media != null) {
			Class<GraphQueryResult> t = GraphQueryResult.class;
			GraphQueryResult result = delegate.readFrom(t, t, media, in,
					charset, base, location, con);
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
