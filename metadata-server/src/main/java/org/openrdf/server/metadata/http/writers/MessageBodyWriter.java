package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;

public interface MessageBodyWriter<T> {

	public long getSize(T result, MediaType mediaType);

	public boolean isWriteable(Class<?> type, MediaType mediaType);

	public String getContentType(Class<?> type, MediaType mediaType);

	public void writeTo(T result, String base, MediaType mediaType,
			OutputStream out) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException;
}
