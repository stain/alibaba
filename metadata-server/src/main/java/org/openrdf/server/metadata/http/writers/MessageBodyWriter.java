package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;

public interface MessageBodyWriter<T> {

	public long getSize(T result, String mimeType);

	public boolean isWriteable(Class<?> type, String mimeType);

	public String getContentType(Class<?> type, String mimeType, Charset charset);

	public void writeTo(T result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException, RepositoryException;
}
