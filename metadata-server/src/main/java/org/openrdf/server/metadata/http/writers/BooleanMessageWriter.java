package org.openrdf.server.metadata.http.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.server.metadata.http.writers.base.MessageWriterBase;

@Provider
public class BooleanMessageWriter
		extends
		MessageWriterBase<BooleanQueryResultFormat, BooleanQueryResultWriterFactory, Boolean> {

	public BooleanMessageWriter() {
		super(BooleanQueryResultWriterRegistry.getInstance(), Boolean.class);
	}

	@Override
	public void writeTo(BooleanQueryResultWriterFactory factory,
			Boolean result, OutputStream out, Charset charset, String base)
			throws IOException {
		factory.getWriter(out).write(result);
	}

}