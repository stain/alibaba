package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


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

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		return findReader(type, genericType, mimeType, con) != null;
	}

	public Object readFrom(Class<? extends Object> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException {
		MessageBodyReader reader = findReader(type, genericType, mimeType, con);
		return reader.readFrom(type, genericType, mimeType, in, charset, base,
				location, con);
	}

	private MessageBodyReader findReader(Class<?> type, Type genericType,
			String mime, ObjectConnection con) {
		for (MessageBodyReader reader : readers) {
			if (reader.isReadable(type, genericType, mime, con)) {
				return reader;
			}
		}
		return null;
	}

}
