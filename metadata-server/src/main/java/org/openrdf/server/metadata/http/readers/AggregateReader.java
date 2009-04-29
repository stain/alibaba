package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public class AggregateReader implements MessageBodyReader<Object> {
	private List<MessageBodyReader> readers = new ArrayList<MessageBodyReader>();

	public AggregateReader() {
		readers.add(new ModelMessageReader());
		readers.add(new GraphMessageReader());
		readers.add(new TupleMessageReader());
		readers.add(new RDFObjectReader());
		readers.add(new SetOfRDFObjectReader());
		readers.add(new StringBodyReader());
	}

	public boolean isReadable(Class<?> type, Type genericType, MediaType media,
			ObjectConnection con) {
		return findReader(type, genericType, media, con) != null;
	}

	public Object readFrom(Class<? extends Object> type, Type genericType,
			MediaType mediaType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException {
		MessageBodyReader reader = findReader(type, genericType, mediaType, con);
		return reader.readFrom(type, genericType, mediaType, in, charset, base,
				location, con);
	}

	private MessageBodyReader findReader(Class<?> type, Type genericType,
			MediaType media, ObjectConnection con) {
		for (MessageBodyReader reader : readers) {
			if (reader.isReadable(type, genericType, media, con)) {
				return reader;
			}
		}
		return null;
	}

}
