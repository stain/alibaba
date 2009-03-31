package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ext.Provider;

import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.result.ContextResult;
import org.openrdf.result.impl.TupleResultImpl;
import org.openrdf.server.metadata.providers.base.MessageWriterBase;

@Provider
public class ContextMessageWriter extends MessageWriterBase<ContextResult> {
	private TupleMessageWriter delegate;

	public ContextMessageWriter(TupleQueryResultWriterFactory factory) {
		super(factory.getTupleQueryResultFormat(), ContextResult.class);
		this.delegate = new TupleMessageWriter(factory);
	}

	@Override
	public void writeTo(ContextResult result, OutputStream out)
			throws Exception {
		List<String> columnNames = Arrays.asList("contextID");
		List<BindingSet> contexts = new ArrayList<BindingSet>();

		try {
			while (result.hasNext()) {
				BindingSet bindingSet = new ListBindingSet(columnNames, result
						.next());
				contexts.add(bindingSet);
			}
		} finally {
			result.close();
		}

		delegate.writeTo(new TupleResultImpl(columnNames, contexts), out);
	}

}
