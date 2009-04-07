package org.openrdf.server.metadata.providers;

import java.io.OutputStream;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.result.TupleResult;
import org.openrdf.result.util.QueryResultUtil;
import org.openrdf.server.metadata.providers.base.ResultMessageWriterBase;

@Provider
public class TupleMessageWriter extends ResultMessageWriterBase<TupleResult> {
	private TupleQueryResultWriterFactory factory;

	public TupleMessageWriter(TupleQueryResultWriterFactory factory) {
		super(factory.getTupleQueryResultFormat(), TupleResult.class);
		this.factory = factory;
	}

	@Override
	public void writeTo(TupleResult result, OutputStream out) throws Exception {
		QueryResultUtil.report(result, factory.getWriter(out));
	}

}
