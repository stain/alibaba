package org.openrdf.server.metadata.providers.base;

import static org.openrdf.http.protocol.Protocol.X_QUERY_TYPE;
import info.aduna.lang.FileFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

public abstract class ResultMessageWriterBase<T extends Result> extends
		MessageWriterBase<T> {
	private String contentType;
	private String queryType;

	public ResultMessageWriterBase(FileFormat format, Class<T> type) {
		super(format, type);
		contentType = format.getDefaultMIMEType();
		if (format.hasCharset()) {
			Charset charset = format.getCharset();
			contentType += "; charset=" + charset.name();
		}
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
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
		} catch (IOException e) {
			throw e;
		} catch (WebApplicationException e) {
			throw e;
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
