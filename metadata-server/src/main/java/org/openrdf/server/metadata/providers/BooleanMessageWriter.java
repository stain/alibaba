package org.openrdf.server.metadata.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class BooleanMessageWriter extends MessageWriterBase<Boolean> {
	private BooleanQueryResultWriterFactory factory;

	public BooleanMessageWriter(BooleanQueryResultWriterFactory factory) {
		super(factory.getBooleanQueryResultFormat(), Boolean.class);
		this.factory = factory;
	}

	public void writeTo(Boolean result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		httpHeaders.putSingle("X-Query-Type", "boolean");
		super.writeTo(result, type, genericType, annotations, mediaType,
				httpHeaders, out);
	}

	@Override
	public void writeTo(Boolean result, OutputStream out, Charset charset)
			throws Exception {
		factory.getWriter(out).write(result);
	}

}
