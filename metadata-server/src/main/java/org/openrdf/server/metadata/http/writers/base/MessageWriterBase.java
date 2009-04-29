package org.openrdf.server.metadata.http.writers.base;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.server.metadata.http.writers.MessageBodyWriter;

public abstract class MessageWriterBase<FF extends FileFormat, S, T> implements
		MessageBodyWriter<T> {
	private FileFormatServiceRegistry<FF, S> registry;
	private Class<T> type;

	public MessageWriterBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		this.registry = registry;
		this.type = type;
	}

	public long getSize(T result, String mimeType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, String mimeType) {
		if (!this.type.isAssignableFrom(type))
			return false;
		return getFactory(mimeType) != null;
	}

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		FF format = getFormat(mimeType);
		String contentType = format.getDefaultMIMEType();
		if (format.hasCharset()) {
			if (charset == null) {
				charset = format.getCharset();
			}
			contentType += "; charset=" + charset.name();
		}
		return contentType;
	}

	public void writeTo(T result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException {
		S factory = getFactory(mimeType);
		writeTo(factory, result, out, charset, base);
	}

	public abstract void writeTo(S factory, T result, OutputStream out,
			Charset charset, String base) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException;

	protected S getFactory(String mimeType) {
		FF format = getFormat(mimeType);
		if (format == null)
			return null;
		return registry.get(format);
	}

	protected FF getFormat(String mimeType) {
		if (mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType)) {
			for (FF format : registry.getKeys()) {
				if (registry.get(format) != null)
					return format;
			}
			return null;
		}
		return registry.getFileFormatForMIMEType(mimeType);
	}
}
