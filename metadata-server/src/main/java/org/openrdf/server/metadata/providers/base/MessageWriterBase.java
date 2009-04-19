package org.openrdf.server.metadata.providers.base;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openrdf.server.metadata.ValueFactoryResource;

import com.sun.jersey.api.core.ResourceContext;

public abstract class MessageWriterBase<FF extends FileFormat, S, T> extends
		MessageProviderBase<FF, S> implements MessageBodyWriter<T> {
	private Class<T> type;
	private ResourceContext ctx;

	public MessageWriterBase(ResourceContext ctx, FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		super(registry);
		this.type = type;
		this.ctx = ctx;
	}

	public long getSize(T result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!this.type.isAssignableFrom(type))
			return false;
		return getFactory(mediaType) != null;
	}

	public void writeTo(T result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		FF format = getFormat(mediaType);
		S factory = getFactory(mediaType);
		String contentType = format.getDefaultMIMEType();
		Charset charset = getCharset(mediaType, format.getCharset());
		if (format.hasCharset()) {
			contentType += "; charset=" + charset.name();
		}
		httpHeaders.putSingle("Content-Type", contentType);
		try {
			String base = "";
			if (ctx != null) {
				base = ctx.getResource(ValueFactoryResource.class).getURI()
				.stringValue();
			}
			writeTo(factory, result, out, charset, base);
		} catch (IOException e) {
			throw e;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	public abstract void writeTo(S factory, T result, OutputStream out,
			Charset charset, String base) throws Exception;

}
