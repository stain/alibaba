package org.openrdf.server.metadata.providers.base;

import info.aduna.lang.FileFormat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

public abstract class MessageReaderBase<T> implements MessageBodyReader<T> {
	private FileFormat format;
	private Class<T> type;

	public MessageReaderBase(FileFormat format, Class<T> type) {
		this.format = format;
		this.type = type;
	}

	@Override
	public String toString() {
		return format.toString();
	}

	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!type.isAssignableFrom(this.type))
			return false;
		if (mediaType == null)
			return false;
		// FIXME FileFormat does not understand MIME parameters
		return format.hasMIMEType(mediaType.getType() + "/"
				+ mediaType.getSubtype());
	}

	public T readFrom(Class<T> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		try {
			return readFrom(in, getCharset(mediaType));
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	public abstract T readFrom(InputStream in, Charset charset) throws Exception;

	private Charset getCharset(MediaType m) {
        String name = (m == null) ? null : m.getParameters().get("charset");
        if (name != null)
        	return Charset.forName(name);
        return null;
    }

}
