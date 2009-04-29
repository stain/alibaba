package org.openrdf.server.metadata.http.writers.base;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

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

	public long getSize(T result, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, MediaType mediaType) {
		if (!this.type.isAssignableFrom(type))
			return false;
		return getFactory(mediaType) != null;
	}

	public String getContentType(Class<?> type, MediaType mediaType) {
		FF format = getFormat(mediaType);
		String contentType = format.getDefaultMIMEType();
		Charset charset = getCharset(mediaType, format.getCharset());
		if (format.hasCharset()) {
			contentType += "; charset=" + charset.name();
		}
		return contentType;
	}

	public void writeTo(T result, String base, MediaType mediaType,
			OutputStream out) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException {
		FF format = getFormat(mediaType);
		S factory = getFactory(mediaType);
		Charset charset = getCharset(mediaType, format.getCharset());
		writeTo(factory, result, out, charset, base);
	}

	public abstract void writeTo(S factory, T result, OutputStream out,
			Charset charset, String base) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException;

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
