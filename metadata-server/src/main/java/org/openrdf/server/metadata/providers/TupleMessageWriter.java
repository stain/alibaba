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

import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.server.metadata.providers.base.ResultMessageWriterBase;

@Provider
public class TupleMessageWriter extends ResultMessageWriterBase<TupleQueryResult> {
	private TupleQueryResultWriterFactory factory;

	public TupleMessageWriter(TupleQueryResultWriterFactory factory) {
		super(factory.getTupleQueryResultFormat(), TupleQueryResult.class);
		this.factory = factory;
	}

	public void writeTo(TupleQueryResult result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		httpHeaders.putSingle("X-Query-Type", "bindings");
		super.writeTo(result, type, genericType, annotations, mediaType,
				httpHeaders, out);
	}

	@Override
	public void writeTo(TupleQueryResult result, OutputStream out, Charset charset) throws Exception {
		QueryResultUtil.report(result, factory.getWriter(out));
	}

}
