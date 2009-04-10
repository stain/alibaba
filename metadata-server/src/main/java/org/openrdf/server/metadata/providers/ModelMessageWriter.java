package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.model.Model;
import org.openrdf.result.GraphResult;
import org.openrdf.result.impl.GraphResultImpl;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class ModelMessageWriter extends MessageWriterBase<Model> {
	private ModelResultMessageWriter delegate;

	public ModelMessageWriter(RDFWriterFactory factory) {
		super(factory.getRDFFormat(), Model.class);
		delegate = new ModelResultMessageWriter(factory);
	}

	@Override
	public void writeTo(Model model, OutputStream out, Charset charset) throws Exception {
		GraphResult result = new GraphResultImpl(model.getNamespaces(), model);
		delegate.writeTo(result, out, charset);
	}
}
