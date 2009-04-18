package org.openrdf.server.metadata.providers.base;

import info.aduna.iteration.CloseableIteration;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.ResourceContext;

public abstract class ResultMessageWriterBase<FF extends FileFormat, S, T extends CloseableIteration<?, ?>>
		extends MessageWriterBase<FF, S, T> {
	private Logger logger = LoggerFactory
			.getLogger(ResultMessageWriterBase.class);
	private String queryType;

	public ResultMessageWriterBase(ResourceContext ctx,
			FileFormatServiceRegistry<FF, S> registry, Class<T> type) {
		super(ctx, registry, type);
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
				logger.warn(e.getMessage(), e);
			}
		}
	}

}
