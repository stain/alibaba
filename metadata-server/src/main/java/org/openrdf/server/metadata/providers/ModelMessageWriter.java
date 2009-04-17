package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.model.Model;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class ModelMessageWriter extends MessageWriterBase<Model> {
	private GraphMessageWriter delegate;

	public ModelMessageWriter(RDFWriterFactory factory) {
		super(factory.getRDFFormat(), Model.class);
		delegate = new GraphMessageWriter(factory);
	}

	@Override
	public void writeTo(Model model, OutputStream out, Charset charset) throws Exception {
		GraphQueryResult result = new GraphQueryResultImpl(model.getNamespaces(), model);
		delegate.writeTo(result, out, charset);
	}
}
