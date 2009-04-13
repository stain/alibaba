package org.openrdf.repository.object.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyLoader {

	private static final String META_INF_ONTOLOGIES = "META-INF/org.openrdf.ontologies";
	private Logger logger = LoggerFactory.getLogger(OntologyLoader.class);
	private Model model = new LinkedHashModel();
	private List<URL> imported = new ArrayList<URL>();
	private ValueFactory vf = ValueFactoryImpl.getInstance();

	public List<URL> getImported() {
		return imported;
	}

	public Model getModel() {
		return model;
	}

	public void loadOntologies(ClassLoader cl) throws IOException,
			RDFParseException {
		Properties ontologies = new Properties();
		Enumeration<URL> resources = cl.getResources(META_INF_ONTOLOGIES);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			InputStream in = url.openStream();
			try {
				ontologies.load(in);
			} finally {
				in.close();
			}
		}
		for (Object key : ontologies.keySet()) {
			URL url = cl.getResource((String) key);
			if (ontologies.get(key) == null) {
				String uri = url.toExternalForm();
				loadOntology(model, url, null, vf.createURI(uri));
			} else {
				String uri = (String) ontologies.get(key);
				loadOntology(model, url, null, vf.createURI(uri));
			}
		}
	}

	public void loadOntologies(List<URL> urls) throws RDFParseException,
			IOException {
		for (URL url : urls) {
			loadOntology(model, url, null, vf.createURI(url.toExternalForm()));
		}
	}

	public void followImports() throws RDFParseException, IOException {
		List<URL> urls = new ArrayList<URL>();
		for (Value obj : model.filter(null, OWL.IMPORTS, null).objects()) {
			if (obj instanceof URI) {
				URI uri = (URI) obj;
				if (!model.contains(null, null, null, uri)) {
					URL url = new URL(uri.stringValue());
					if (!imported.contains(url)) {
						urls.add(url);
					}
				}
			}
		}
		if (!urls.isEmpty()) {
			imported.addAll(urls);
			for (URL url : urls) {
				String uri = url.toExternalForm();
				loadOntology(model, url, null, vf.createURI(uri));
			}
			followImports();
		}
	}

	private void loadOntology(Model model, URL url, RDFFormat override,
			final URI uri) throws IOException, RDFParseException {
		URLConnection conn = url.openConnection();
		if (override == null) {
			conn.setRequestProperty("Accept", getAcceptHeader());
		} else {
			conn.setRequestProperty("Accept", override.getDefaultMIMEType());
		}
		RDFFormat format = override;
		if (override == null) {
			format = RDFFormat.RDFXML;
			format = RDFFormat.forFileName(url.toString(), format);
			format = RDFFormat.forMIMEType(conn.getContentType(), format);
		}
		RDFParserRegistry registry = RDFParserRegistry.getInstance();
		RDFParser parser = registry.get(format).getParser();
		parser.setRDFHandler(new StatementCollector(model) {
			@Override
			public void handleStatement(Statement st) {
				Resource s = st.getSubject();
				URI p = st.getPredicate();
				Value o = st.getObject();
				super.handleStatement(new StatementImpl(s, p, o, uri));
			}
		});
		try {
			InputStream in = conn.getInputStream();
			try {
				parser.parse(in, url.toExternalForm());
			} catch (RDFHandlerException e) {
				throw new AssertionError(e);
			} catch (RDFParseException e) {
				if (override == null && format.equals(RDFFormat.NTRIPLES)) {
					// sometimes text/plain is used for rdf+xml
					loadOntology(model, url, RDFFormat.RDFXML, uri);
				} else {
					throw e;
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			logger.warn("Could not load {} {}", url, e.getMessage());
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
