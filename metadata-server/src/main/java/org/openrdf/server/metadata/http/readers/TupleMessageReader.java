package org.openrdf.server.metadata.http.readers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.query.resultio.TupleQueryResultParserFactory;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.server.metadata.http.readers.base.MessageReaderBase;

@Provider
public class TupleMessageReader
		extends
		MessageReaderBase<TupleQueryResultFormat, TupleQueryResultParserFactory, TupleQueryResult> {

	public TupleMessageReader() {
		super(TupleQueryResultParserRegistry.getInstance(),
				TupleQueryResult.class);
	}

	@Override
	public TupleQueryResult readFrom(TupleQueryResultParserFactory factory,
			InputStream in, Charset charset, String base)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			IOException {
		TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
		TupleQueryResultParser parser = factory.getParser();
		parser.setTupleQueryResultHandler(builder);
		parser.parse(in);
		return builder.getQueryResult();
	}

}
