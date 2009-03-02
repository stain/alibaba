package org.openrdf.repository.object.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.StatementCollector;

public class OntologyLoader {

	public Model loadOntologies(List<URL> urls, boolean follow)
			throws RDFParseException, IOException {
		Model model = new LinkedHashModel();
		loadOntologyList(urls, model, follow);
		return model;
	}

	private void loadOntologyList(List<URL> ontologyUrls, Model model,
			boolean followImports) throws IOException, RDFParseException {
		for (URL url : ontologyUrls) {
			loadOntology(model, url, null);
		}
		if (followImports) {
			List<URL> urls = new ArrayList<URL>();
			for (Value obj : model.filter(null, OWL.IMPORTS, null).objects()) {
				if (obj instanceof URI) {
					URI uri = (URI) obj;
					if (!model.contains(null, null, null, uri)) {
						urls.add(new URL(uri.stringValue()));
					}
				}
			}
			if (!urls.isEmpty()) {
				loadOntologyList(urls, model, followImports);
			}
		}
	}

	private void loadOntology(Model model, URL url, RDFFormat override)
			throws IOException, RDFParseException {
		URLConnection conn = url.openConnection();
		if (override == null) {
			conn.setRequestProperty("Accept", getAcceptHeader());
		} else {
			conn.setRequestProperty("Accept", override.getDefaultMIMEType());
		}
		ValueFactory vf = ValueFactoryImpl.getInstance();
		RDFFormat format = override;
		if (override == null) {
			format = RDFFormat.RDFXML;
			format = RDFFormat.forFileName(url.toString(), format);
			format = RDFFormat.forMIMEType(conn.getContentType(), format);
		}
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry.get(format).getParser();
		final URI uri = vf.createURI(url.toExternalForm());
		parser.setRDFHandler(new StatementCollector(model) {
			@Override
			public void handleStatement(Statement st) {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				super.handleStatement(new StatementImpl(s, p, o, uri));
			}
		});
		InputStream in = conn.getInputStream();
		try {
			parser.parse(in, url.toExternalForm());
		} catch (RDFHandlerException e) {
			throw new AssertionError(e);
		} catch (RDFParseException e) {
			if (override == null && format.equals(RDFFormat.NTRIPLES)) {
				// sometimes text/plain is used for rdf+xml
				loadOntology(model, url, RDFFormat.RDFXML);
			} else {
				throw e;
			}
		} finally {
			in.close();
		}
	}

	private String getAcceptHeader() {
		StringBuilder sb = new StringBuilder();
		String preferred = RDFFormat.RDFXML.getDefaultMIMEType();
		sb.append(preferred).append(";q=0.2");
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		for (RDFFormat format : rdfFormats) {
			for (String type : format.getMIMETypes()) {
				if (!preferred.equals(type)) {
					sb.append(", ").append(type);
				}
			}
		}
		return sb.toString();
	}
}
