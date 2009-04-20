package org.openrdf.server.metadata.providers.base;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.openrdf.server.metadata.ValueFactoryResource;

import com.sun.jersey.api.core.ResourceContext;

public abstract class MessageReaderBase<FF extends FileFormat, S, T> extends
		MessageProviderBase<FF, S> implements MessageBodyReader<T> {
	private Class<T> type;
	private ResourceContext ctx;

	public MessageReaderBase(ResourceContext ctx,
			FileFormatServiceRegistry<FF, S> registry, Class<T> type) {
		super(registry);
		this.type = type;
		this.ctx = ctx;
	}

	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType media) {
		if (Object.class.equals(type))
			return false;
		if (!type.isAssignableFrom(this.type))
			return false;
		if (media == null || WILDCARD_TYPE.equals(media)
				|| APPLICATION_OCTET_STREAM_TYPE.equals(media))
			return false;
		return getFactory(media) != null;
	}

	public T readFrom(Class<T> type, Type genericType,
			Annotation[] annotations, MediaType media,
			MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		try {
			String base = "";
			if (httpHeaders.containsKey("Content-Location")) {
				base = httpHeaders.getFirst("Content-Location");
			}
			if (ctx != null) {
				ValueFactoryResource vf = ctx.getResource(ValueFactoryResource.class);
				if (base.length() == 0) {
					base = vf.getURI().stringValue();
				} else {
					base = vf.createURI(base).stringValue();
				}
			}
			return readFrom(getFactory(media), in, getCharset(media, null),
					base);
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	public abstract T readFrom(S factory, InputStream in, Charset charset,
			String base) throws Exception;

}
