package org.openrdf.server.metadata.providers;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.query.resultio.TupleQueryResultParserFactory;
import org.openrdf.server.metadata.providers.base.MessageReaderBase;

@Provider
public class TupleMessageReader extends MessageReaderBase<TupleQueryResult> {
	private TupleQueryResultParserFactory factory;

	public TupleMessageReader(TupleQueryResultParserFactory factory) {
		super(factory.getTupleQueryResultFormat(), TupleQueryResult.class);
		this.factory = factory;
	}

	@Override
	public TupleQueryResult readFrom(InputStream in, Charset charset) throws Exception {
		TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
		TupleQueryResultParser parser = factory.getParser();
		parser.setTupleQueryResultHandler(builder);
		parser.parse(in);
		return builder.getQueryResult();
	}

}
