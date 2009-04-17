package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.nio.charset.Charset;

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

	@Override
	public void writeTo(TupleQueryResult result, OutputStream out, Charset charset) throws Exception {
		QueryResultUtil.report(result, factory.getWriter(out));
	}

}
