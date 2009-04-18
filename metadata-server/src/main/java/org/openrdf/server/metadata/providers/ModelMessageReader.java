package org.openrdf.server.metadata.providers;

import info.aduna.iteration.Iterations;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.server.metadata.providers.base.MessageReaderBase;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class ModelMessageReader extends
		MessageReaderBase<RDFFormat, RDFParserFactory, Model> {
	private GraphMessageReader delegate;

	public ModelMessageReader(@Context ResourceContext ctx) {
		super(ctx, RDFParserRegistry.getInstance(), Model.class);
		delegate = new GraphMessageReader(ctx);
	}

	@Override
	public Model readFrom(RDFParserFactory factory, InputStream in,
			Charset charset, String base) throws Exception {
		GraphQueryResult result = delegate.readFrom(factory, in, charset, base);
		return new LinkedHashModel(result.getNamespaces(), Iterations
				.asList(result));
	}
}
