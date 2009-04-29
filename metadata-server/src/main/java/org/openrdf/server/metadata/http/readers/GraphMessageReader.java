package org.openrdf.server.metadata.http.readers;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.server.metadata.helpers.BackgroundGraphResult;
import org.openrdf.server.metadata.http.readers.base.MessageReaderBase;

public class GraphMessageReader extends
		MessageReaderBase<RDFFormat, RDFParserFactory, GraphQueryResult> {
	private static ExecutorService executor = Executors.newFixedThreadPool(3);

	public GraphMessageReader() {
		super(RDFParserRegistry.getInstance(), GraphQueryResult.class);
	}

	@Override
	public GraphQueryResult readFrom(RDFParserFactory factory, InputStream in,
			Charset charset, String base) {
		RDFParser parser = factory.getParser();
		BackgroundGraphResult result = new BackgroundGraphResult(parser, in,
				charset, base);
		executor.execute(result);
		return result;
	}

}
