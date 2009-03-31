package org.openrdf.server.metadata.providers.base;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.openrdf.http.protocol.Protocol.X_QUERY_TYPE;
import info.aduna.io.file.FileFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

public abstract class MessageWriterBase<T extends Result> implements
		MessageBodyWriter<T> {
	private FileFormat format;
	private Class<T> type;
	private String contentType;
	private String queryType;

	public MessageWriterBase(FileFormat format, Class<T> type) {
		this.format = format;
		this.type = type;
		contentType = format.getDefaultMIMEType();
		if (format.hasCharset()) {
			Charset charset = format.getCharset();
			contentType += "; charset=" + charset.name();
		}
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	@Override
	public String toString() {
		return format.toString();
	}

	public long getSize(T result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!this.type.isAssignableFrom(type))
			return false;
		if (mediaType == null || WILDCARD_TYPE.equals(mediaType)
				|| APPLICATION_OCTET_STREAM_TYPE.equals(mediaType))
			return true;
		// FIXME FileFormat does not understand MIME parameters
		return format.hasMIMEType(mediaType.getType() + "/"
				+ mediaType.getSubtype());
	}

	public void writeTo(T result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		httpHeaders.putSingle("Content-Type", contentType);
		if (queryType != null) {
			httpHeaders.putSingle(X_QUERY_TYPE, queryType);
		}
		try {
			writeTo(result, out);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		} finally {
			try {
				result.close();
			} catch (StoreException e) {
				// TODO logger
				e.printStackTrace();
			}
		}
	}

	public abstract void writeTo(T result, OutputStream out) throws Exception;

}
