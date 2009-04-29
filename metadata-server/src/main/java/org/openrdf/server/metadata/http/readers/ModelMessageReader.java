package org.openrdf.server.metadata.http.readers;

import info.aduna.iteration.Iterations;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.server.metadata.http.readers.base.MessageReaderBase;

public class ModelMessageReader extends
		MessageReaderBase<RDFFormat, RDFParserFactory, Model> {
	private GraphMessageReader delegate;

	public ModelMessageReader() {
		super(RDFParserRegistry.getInstance(), Model.class);
		delegate = new GraphMessageReader();
	}

	@Override
	public Model readFrom(RDFParserFactory factory, InputStream in,
			Charset charset, String base) throws QueryEvaluationException {
		GraphQueryResult result = delegate.readFrom(factory, in, charset, base);
		return new LinkedHashModel(result.getNamespaces(), Iterations
				.asList(result));
	}
}
