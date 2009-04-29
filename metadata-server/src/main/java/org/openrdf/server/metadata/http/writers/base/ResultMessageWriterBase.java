package org.openrdf.server.metadata.http.writers.base;

import info.aduna.iteration.CloseableIteration;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResultMessageWriterBase<FF extends FileFormat, S, T extends CloseableIteration<?, ?>>
		extends MessageWriterBase<FF, S, T> {
	private Logger logger = LoggerFactory
			.getLogger(ResultMessageWriterBase.class);

	public ResultMessageWriterBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		super(registry, type);
	}

	@Override
	public void writeTo(T result, String base, MediaType mediaType,
			OutputStream out) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException {
		try {
			super.writeTo(result, base, mediaType, out);
		} finally {
			try {
				result.close();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

}
