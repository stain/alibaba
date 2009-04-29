package org.openrdf.server.metadata.http.readers.base;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

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

	public boolean isReadable(Class<?> type, Type genericType, MediaType media,
			ObjectConnection con) {
		if (Object.class.equals(type))
			return false;
		if (!type.equals(this.type))
			return false;
		if (media == null || WILDCARD_TYPE.equals(media)
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media))
			return false;
		return getFactory(media) != null;
	}

	public T readFrom(Class<? extends T> type, Type genericType,
			MediaType media, InputStream in, Charset charset, String base,
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

	protected S getFactory(MediaType media) {
		FF format = getFormat(media);
		if (format == null)
			return null;
		return registry.get(format);
	}

	protected FF getFormat(MediaType media) {
		if (media == null || media.isWildcardType()
				&& media.isWildcardSubtype()
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media)) {
			for (FF format : registry.getKeys()) {
				if (registry.get(format) != null)
					return format;
			}
			return null;
		}
		// FIXME FileFormat does not understand MIME parameters
		String mimeType = media.getType() + "/" + media.getSubtype();
		return registry.getFileFormatForMIMEType(mimeType);
	}

	protected Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

}
