package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.openrdf.model.Model;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

import com.sun.jersey.api.core.ResourceContext;

@Provider
public class ModelMessageWriter extends
		MessageWriterBase<RDFFormat, RDFWriterFactory, Model> {
	private GraphMessageWriter delegate;

	public ModelMessageWriter(@Context ResourceContext ctx) {
		super(ctx, RDFWriterRegistry.getInstance(), Model.class);
		delegate = new GraphMessageWriter(ctx);
	}

	@Override
	public void writeTo(RDFWriterFactory factory, Model model,
			OutputStream out, Charset charset, String base) throws Exception {
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		delegate.writeTo(factory, result, out, charset, base);
	}
}
