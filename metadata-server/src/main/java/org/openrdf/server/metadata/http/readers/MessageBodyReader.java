package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

public interface MessageBodyReader<T> {

	boolean isReadable(Class<?> type, Type genericType, MediaType media,
			ObjectConnection con);

	T readFrom(Class<? extends T> type, Type genericType, MediaType mediaType,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws QueryResultParseException,
			TupleQueryResultHandlerException, QueryEvaluationException,
			IOException, RepositoryException;
}
