package org.openrdf.server.metadata.providers;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.query.resultio.TupleQueryResultParserFactory;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.server.metadata.providers.base.MessageReaderBase;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class TupleMessageReader
		extends
		MessageReaderBase<TupleQueryResultFormat, TupleQueryResultParserFactory, TupleQueryResult> {

	public TupleMessageReader(@Context ResourceContext ctx) {
		super(ctx, TupleQueryResultParserRegistry.getInstance(),
				TupleQueryResult.class);
	}

	@Override
	public TupleQueryResult readFrom(TupleQueryResultParserFactory factory,
			InputStream in, Charset charset, String base) throws Exception {
		TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
		TupleQueryResultParser parser = factory.getParser();
		parser.setTupleQueryResultHandler(builder);
		parser.parse(in);
		return builder.getQueryResult();
	}

}
