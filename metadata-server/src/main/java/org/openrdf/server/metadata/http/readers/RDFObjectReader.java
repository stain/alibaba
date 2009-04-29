package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

public class RDFObjectReader implements MessageBodyReader<Object> {
	private GraphMessageReader delegate;

	public RDFObjectReader() {
		delegate = new GraphMessageReader();
	}

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		Class<GraphQueryResult> t = GraphQueryResult.class;
		if (mediaType != null && !delegate.isReadable(t, t, mediaType, con))
			return false;
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return true;
		if (RDFObject.class.isAssignableFrom(type))
			return true;
		return con.getObjectFactory().isNamedConcept(type);
	}

	public Object readFrom(Class<?> type, Type genericType, String media,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws QueryResultParseException,
			TupleQueryResultHandlerException, IOException,
			QueryEvaluationException, RepositoryException {
		Resource subj = null;
		if (location != null) {
			ValueFactory vf = con.getValueFactory();
			subj = vf.createURI(location);
		}
		if (media != null) {
			Class<GraphQueryResult> t = GraphQueryResult.class;
			GraphQueryResult result = delegate.readFrom(t, t, media, in,
					charset, base, location, con);
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
		}
		return con.getObject(subj);
	}

}
