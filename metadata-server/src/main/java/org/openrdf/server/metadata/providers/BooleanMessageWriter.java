package org.openrdf.server.metadata.providers;

import static org.openrdf.http.protocol.Protocol.BOOLEAN_QUERY;

import java.io.OutputStream;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.result.BooleanResult;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class BooleanMessageWriter extends MessageWriterBase<BooleanResult> {
	private BooleanQueryResultWriterFactory factory;

	public BooleanMessageWriter(BooleanQueryResultWriterFactory factory) {
		super(factory.getBooleanQueryResultFormat(), BooleanResult.class);
		this.factory = factory;
		setQueryType(BOOLEAN_QUERY);
	}

	@Override
	public void writeTo(BooleanResult result, OutputStream out)
			throws Exception {
		factory.getWriter(out).write(result.asBoolean());
	}

}
