package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;

public class AggregateWriter implements MessageBodyWriter<Object> {
	private List<MessageBodyWriter> writers = new ArrayList<MessageBodyWriter>();

	public AggregateWriter() {
		writers.add(new FileBodyWriter());
		writers.add(new BooleanMessageWriter());
		writers.add(new ModelMessageWriter());
		writers.add(new GraphMessageWriter());
		writers.add(new TupleMessageWriter());
		writers.add(new RDFObjectWriter());
		writers.add(new SetOfRDFObjectWriter());
		writers.add(new StringBodyWriter());
	}

	public String getContentType(Class<?> type, MediaType mediaType) {
		return findWriter(type, mediaType).getContentType(type, mediaType);
	}

	public long getSize(Object result, MediaType mediaType) {
		return findWriter(result.getClass(), mediaType).getSize(result,
				mediaType);
	}

	public boolean isWriteable(Class<?> type, MediaType mediaType) {
		return findWriter(type, mediaType) != null;
	}

	public void writeTo(Object result, String base, MediaType mediaType,
			OutputStream out) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException {
		MessageBodyWriter writer = findWriter(result.getClass(), mediaType);
		writer.writeTo(result, base, mediaType, out);
	}

	private MessageBodyWriter findWriter(Class<?> type, MediaType mediaType) {
		for (MessageBodyWriter w : writers) {
			if (w.isWriteable(type, mediaType)) {
				return w;
			}
		}
		return null;
	}

}
