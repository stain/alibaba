package org.openrdf.server.metadata.http.readers.base;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.http.readers.MessageBodyReader;

public abstract class MessageReaderBase<FF extends FileFormat, S, T> implements
		MessageBodyReader<T> {
	private FileFormatServiceRegistry<FF, S> registry;
	private Class<T> type;

	public MessageReaderBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		this.registry = registry;
		this.type = type;
	}

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		if (Object.class.equals(type))
			return false;
		if (!type.equals(this.type))
			return false;
		if (mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType))
			return false;
		return getFactory(mimeType) != null;
	}

	public T readFrom(Class<? extends T> type, Type genericType,
			String media, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			IOException, QueryEvaluationException {
		if (location != null) {
			base = location;
		}
		return readFrom(getFactory(media), in, charset, base);
	}

	public abstract T readFrom(S factory, InputStream in, Charset charset,
			String base) throws QueryResultParseException,
			TupleQueryResultHandlerException, IOException,
			QueryEvaluationException;

	protected S getFactory(String mime) {
		FF format = getFormat(mime);
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
