package org.openrdf.server.metadata.providers;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.Provider;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.result.ModelResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.server.metadata.providers.base.ResultMessageWriterBase;

@Provider
public class ModelResultMessageWriter extends ResultMessageWriterBase<ModelResult> {
	private static final int SMALL = 16;
	private RDFWriterFactory factory;

	public ModelResultMessageWriter(RDFWriterFactory factory) {
		super(factory.getRDFFormat(), ModelResult.class);
		this.factory = factory;
	}

	@Override
	public void writeTo(ModelResult result, OutputStream out, Charset charset) throws Exception {
		RDFFormat rdfFormat = factory.getRDFFormat();
		RDFWriter writer = getWriter(out, charset);
		// TODO writer.setBaseURI(req.getRequestURL().toString());
		writer.startRDF();

		Set<String> firstNamespaces = null;
		List<Statement> firstStatements = new ArrayList<Statement>(SMALL);

		// Only try to trim namespace if the RDF format supports
		// namespaces
		// in the first place
		boolean trimNamespaces = rdfFormat.supportsNamespaces();

		if (trimNamespaces) {
			// Gather the first few statements
			for (int i = 0; result.hasNext() && i < SMALL; i++) {
				firstStatements.add(result.next());
			}

			// Only trim namespaces if the set is small enough
			trimNamespaces = firstStatements.size() < SMALL;

			if (trimNamespaces) {
				// Gather the namespaces from the first few statements
				firstNamespaces = new HashSet<String>(SMALL);

				for (Statement st : firstStatements) {
					addNamespace(st.getSubject(), firstNamespaces);
					addNamespace(st.getPredicate(), firstNamespaces);
					addNamespace(st.getObject(), firstNamespaces);
					addNamespace(st.getContext(), firstNamespaces);
				}
			}
		}

		// Report namespace prefixes
		for (Map.Entry<String, String> ns : result.getNamespaces().entrySet()) {
			String prefix = ns.getKey();
			String namespace = ns.getValue();
			if (trimNamespaces == false || firstNamespaces.contains(namespace)) {
				writer.handleNamespace(prefix, namespace);
			}
		}

		// Report statements
		for (Statement st : firstStatements) {
			writer.handleStatement(st);
		}

		while (result.hasNext()) {
			Statement st = result.next();
			writer.handleStatement(st);
		}

		writer.endRDF();
	}

	private RDFWriter getWriter(OutputStream out, Charset charset) {
		if (charset == null)
			return factory.getWriter(out);
		return factory.getWriter(new OutputStreamWriter(out, charset));
	}

	private void addNamespace(Value value, Set<String> namespaces) {
		if (value instanceof URI) {
			URI uri = (URI) value;
			namespaces.add(uri.getNamespace());
		}
	}

}
