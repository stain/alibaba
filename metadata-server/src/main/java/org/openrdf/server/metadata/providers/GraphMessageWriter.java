package org.openrdf.server.metadata.providers;

import static org.openrdf.http.protocol.Protocol.GRAPH_QUERY;

import java.io.OutputStream;

import javax.ws.rs.ext.Provider;

import org.openrdf.result.GraphResult;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class GraphMessageWriter extends MessageWriterBase<GraphResult> {
	private ModelMessageWriter delegate;

	public GraphMessageWriter(RDFWriterFactory factory) {
		super(factory.getRDFFormat(), GraphResult.class);
		this.delegate = new ModelMessageWriter(factory);
		setQueryType(GRAPH_QUERY);
	}

	@Override
	public void writeTo(GraphResult result, OutputStream out) throws Exception {
		delegate.writeTo(result, out);
	}

}
