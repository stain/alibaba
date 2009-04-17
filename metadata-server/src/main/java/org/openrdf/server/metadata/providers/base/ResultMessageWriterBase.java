package org.openrdf.server.metadata.providers.base;

import info.aduna.iteration.CloseableIteration;
import info.aduna.lang.FileFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public abstract class ResultMessageWriterBase<T extends CloseableIteration<?,?>> extends
		MessageWriterBase<T> {
	private String queryType;

	public ResultMessageWriterBase(FileFormat format, Class<T> type) {
		super(format, type);
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public void writeTo(T result, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		try {
			if (queryType != null) {
				httpHeaders.putSingle("X-Query-Type", queryType);
			}
			super.writeTo(result, type, genericType, annotations, mediaType,
					httpHeaders, out);
		} finally {
			try {
				result.close();
			} catch (Exception e) {
				// TODO logger
				e.printStackTrace();
			}
		}
	}

	public abstract void writeTo(T result, OutputStream out, Charset charset)
			throws Exception;

}
