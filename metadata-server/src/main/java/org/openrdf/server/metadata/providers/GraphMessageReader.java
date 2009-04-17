package org.openrdf.server.metadata.providers;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.server.metadata.helpers.BackgroundGraphResult;
import org.openrdf.server.metadata.providers.base.MessageReaderBase;

@Provider
public class GraphMessageReader extends MessageReaderBase<GraphQueryResult> {
	private static ExecutorService executor = Executors.newFixedThreadPool(3);
	private RDFParserFactory factory;

	public GraphMessageReader(RDFParserFactory factory) {
		super(factory.getRDFFormat(), GraphQueryResult.class);
		this.factory = factory;
	}

	@Override
	public GraphQueryResult readFrom(InputStream in, Charset charset) throws Exception {
		String base = ""; // TODO
		RDFParser parser = factory.getParser();
		BackgroundGraphResult result = new BackgroundGraphResult(parser, in, charset, base);
		executor.execute(result);
		return result;
	}

}
