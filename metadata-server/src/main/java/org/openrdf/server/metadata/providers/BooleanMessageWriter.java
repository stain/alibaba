package org.openrdf.server.metadata.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class BooleanMessageWriter
		extends
		MessageWriterBase<BooleanQueryResultFormat, BooleanQueryResultWriterFactory, Boolean> {

	public BooleanMessageWriter(@Context ResourceContext ctx) {
		super(ctx, BooleanQueryResultWriterRegistry.getInstance(), Boolean.class);
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
	public void writeTo(BooleanQueryResultWriterFactory factory, Boolean result, OutputStream out,
			Charset charset, String base) throws Exception {
		factory.getWriter(out).write(result);
	}

}
