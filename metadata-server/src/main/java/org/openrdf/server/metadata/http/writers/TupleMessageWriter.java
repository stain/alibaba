package org.openrdf.server.metadata.http.writers;

import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
import org.openrdf.server.metadata.http.writers.base.ResultMessageWriterBase;

@Provider
public class TupleMessageWriter
		extends
		ResultMessageWriterBase<TupleQueryResultFormat, TupleQueryResultWriterFactory, TupleQueryResult> {

	public TupleMessageWriter() {
		super(TupleQueryResultWriterRegistry.getInstance(),
				TupleQueryResult.class);
	}

	@Override
	public void writeTo(TupleQueryResultWriterFactory factory,
			TupleQueryResult result, OutputStream out, Charset charset,
			String base) throws TupleQueryResultHandlerException,
			QueryEvaluationException {
		QueryResultUtil.report(result, factory.getWriter(out));
	}

}
