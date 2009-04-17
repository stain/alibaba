package org.openrdf.server.metadata;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.query.resultio.TupleQueryResultParserFactory;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.server.metadata.providers.BooleanMessageWriter;
import org.openrdf.server.metadata.providers.GraphMessageReader;
import org.openrdf.server.metadata.providers.GraphMessageWriter;
import org.openrdf.server.metadata.providers.ModelMessageReader;
import org.openrdf.server.metadata.providers.ModelMessageWriter;
import org.openrdf.server.metadata.providers.TupleMessageReader;
import org.openrdf.server.metadata.providers.TupleMessageWriter;

public class MessageProviderFactory {

	public Set<Object> getAll() {
		Set<Object> providers = new HashSet<Object>();
		addGraphWriters(providers);
		addGraphReaders(providers);
		addTupleWriters(providers);
		addTupleReaders(providers);
		addBooleanWriters(providers);
		return providers;
	}

	private void addGraphWriters(Set<Object> providers) {
		RDFWriterRegistry registry = RDFWriterRegistry.getInstance();
		for (RDFWriterFactory factory : registry.getAll()) {
			providers.add(new GraphMessageWriter(factory));
			providers.add(new ModelMessageWriter(factory));
		}
	}

	private void addGraphReaders(Set<Object> providers) {
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		for (RDFParserFactory factory : registry.getAll()) {
			providers.add(new GraphMessageReader(factory));
			providers.add(new ModelMessageReader(factory));
		}
	}

	private void addTupleWriters(Set<Object> providers) {
		TupleQueryResultWriterRegistry registry;
		registry = TupleQueryResultWriterRegistry.getInstance();
		for (TupleQueryResultWriterFactory factory : registry.getAll()) {
			providers.add(new TupleMessageWriter(factory));
		}
	}

	private void addTupleReaders(Set<Object> providers) {
		TupleQueryResultParserRegistry registry;
		registry = TupleQueryResultParserRegistry.getInstance();
		for (TupleQueryResultParserFactory factory : registry.getAll()) {
			providers.add(new TupleMessageReader(factory));
		}
	}

	private void addBooleanWriters(Set<Object> providers) {
		BooleanQueryResultWriterRegistry registry;
		registry = BooleanQueryResultWriterRegistry.getInstance();
		for (BooleanQueryResultWriterFactory factory : registry.getAll()) {
			providers.add(new BooleanMessageWriter(factory));
		}
	}
}
