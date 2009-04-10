package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ext.Provider;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.result.NamespaceResult;
import org.openrdf.result.impl.TupleResultImpl;
import org.openrdf.server.metadata.providers.base.ResultMessageWriterBase;

@Provider
public class NamespaceMessageWriter extends
		ResultMessageWriterBase<NamespaceResult> {
	private TupleMessageWriter delegate;

	public NamespaceMessageWriter(TupleQueryResultWriterFactory factory) {
		super(factory.getTupleQueryResultFormat(), NamespaceResult.class);
		this.delegate = new TupleMessageWriter(factory);
	}

	@Override
	public void writeTo(NamespaceResult result, OutputStream out, Charset charset)
			throws Exception {
		List<String> columnNames = Arrays.asList(Protocol.PREFIX,
				Protocol.NAMESPACE);
		List<BindingSet> namespaces = new ArrayList<BindingSet>();

		try {
			while (result.hasNext()) {
				Namespace ns = result.next();

				Literal prefix = new LiteralImpl(ns.getPrefix());
				Literal namespace = new LiteralImpl(ns.getName());

				BindingSet bindingSet = new ListBindingSet(columnNames, prefix,
						namespace);
				namespaces.add(bindingSet);
			}
		} finally {
			result.close();
		}

		delegate.writeTo(new TupleResultImpl(columnNames, namespaces), out, charset);
	}

}
