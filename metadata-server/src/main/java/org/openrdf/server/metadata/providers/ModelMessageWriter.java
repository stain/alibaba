package org.openrdf.server.metadata.providers;

import java.io.OutputStream;

import javax.ws.rs.ext.Provider;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.result.impl.ModelResultImpl;
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
	public void writeTo(Model model, OutputStream out) throws Exception {
		CollectionCursor<Statement> cursor;
		cursor = new CollectionCursor<Statement>(model);
		delegate.writeTo(new ModelResultImpl(cursor), out);
	}
}
