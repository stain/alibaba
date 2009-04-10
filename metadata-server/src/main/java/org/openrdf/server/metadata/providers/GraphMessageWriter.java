package org.openrdf.server.metadata.providers;

import static org.openrdf.http.protocol.Protocol.GRAPH_QUERY;

import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.ext.Provider;

import org.openrdf.result.GraphResult;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.server.metadata.providers.base.ResultMessageWriterBase;

@Provider
public class GraphMessageWriter extends ResultMessageWriterBase<GraphResult> {
	private ModelResultMessageWriter delegate;

	public GraphMessageWriter(RDFWriterFactory factory) {
		super(factory.getRDFFormat(), GraphResult.class);
		this.delegate = new ModelResultMessageWriter(factory);
		setQueryType(GRAPH_QUERY);
	}

	@Override
	public void writeTo(GraphResult result, OutputStream out, Charset charset) throws Exception {
		delegate.writeTo(result, out, charset);
	}

}
