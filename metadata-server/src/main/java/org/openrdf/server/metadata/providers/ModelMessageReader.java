package org.openrdf.server.metadata.providers;

import info.aduna.iteration.Iterations;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.server.metadata.providers.base.MessageReaderBase;

@Provider
public class ModelMessageReader extends MessageReaderBase<Model> {
	private GraphMessageReader delegate;

	public ModelMessageReader(RDFParserFactory factory) {
		super(factory.getRDFFormat(), Model.class);
		delegate = new GraphMessageReader(factory);
	}

	@Override
	public Model readFrom(InputStream in, Charset charset) throws Exception {
		GraphQueryResult result = delegate.readFrom(in, charset);
		return new LinkedHashModel(result.getNamespaces(), Iterations.asList(result));
	}
}
