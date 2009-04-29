package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


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

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		return findWriter(type, mimeType).getContentType(type, mimeType, charset);
	}

	public long getSize(Object result, String mimeType) {
		return findWriter(result.getClass(), mimeType).getSize(result, mimeType);
	}

	public boolean isWriteable(Class<?> type, String mimeType) {
		return findWriter(type, mimeType) != null;
	}

	public void writeTo(Object result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException {
		MessageBodyWriter writer = findWriter(result.getClass(), mimeType);
		writer.writeTo(result, base, mimeType, out, charset);
	}

	private MessageBodyWriter findWriter(Class<?> type, String mimeType) {
		for (MessageBodyWriter w : writers) {
			if (w.isWriteable(type, mimeType)) {
				return w;
			}
		}
		return null;
	}

}
