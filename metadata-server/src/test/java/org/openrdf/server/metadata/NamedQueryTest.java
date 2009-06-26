package org.openrdf.server.metadata;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class NamedQueryTest extends MetadataServerTestCase {

	@rdf("http://www.openrdf.org/rdf/2009/metadata#NamedQuery")
	public interface NamedQuery {

		@rdf("http://www.openrdf.org/rdf/2009/metadata#inSparql")
		String getMetaInSparql();

		void setMetaInSparql(String sparql);

		@rdf("http://www.openrdf.org/rdf/2009/metadata#parameter")
		Set<Parameter> getMetaParameters();

		void setMetaParameters(Set<Parameter> parameters);
	}

	@rdf("http://www.openrdf.org/rdf/2009/metadata#Parameter")
	public interface Parameter {

		@rdf("http://www.openrdf.org/rdf/2009/metadata#name")
		String getMetaName();
		void setMetaName(String name);

		@rdf("http://www.openrdf.org/rdf/2009/metadata#base")
		Object getMetaBase();
		void setMetaBase(Object base);

		@rdf("http://www.openrdf.org/rdf/2009/metadata#datatype")
		Object getMetaDatatype();
		void setMetaDatatype(Object datatype);

	}

	public static abstract class NamedQuerySupport implements NamedQuery, RDFObject {

		@title("Evaluate Query")
		@operation("evaluate")
		public GraphQueryResult metaEvaluate(@parameter("*") Map<String, String[]> parameters)
				throws RepositoryException, URISyntaxException,
				QueryEvaluationException, MalformedQueryException {
			String sparql = getMetaInSparql();
			RepositoryConnection con = getObjectConnection();
			ValueFactory vf = con.getValueFactory();
			Query query = con.prepareQuery(SPARQL, sparql);
			for (Parameter parameter : getMetaParameters()) {
				String name = parameter.getMetaName();
				String[] values = parameters.get(name);
				if (values == null || values.length < 1)
					continue;
				String value = values[0];
				RDFObject base = (RDFObject) parameter.getMetaBase();
				RDFObject datatype = (RDFObject) parameter.getMetaDatatype();
				if (datatype != null) {
					Resource dt = datatype.getResource();
					query.setBinding(name, vf.createLiteral(value, (URI) dt));
				} else if (base != null) {
					String baseURI = ((RDFObject) base).getResource()
							.stringValue();
					if (baseURI.endsWith("#") || baseURI.endsWith("/")) {
						query.setBinding(name, vf.createURI(baseURI, value));
					} else if (getResource() instanceof URI) {
						java.net.URI uri = new java.net.URI(baseURI);
						uri = uri.resolve(value);
						query.setBinding(name, vf
								.createURI(uri.toASCIIString()));
					} else {
						query.setBinding(name, vf.createURI(value));
					}
				} else {
					query.setBinding(name, vf.createLiteral(value));
				}
			}
			if (query instanceof GraphQuery) {
				return ((GraphQuery) query).evaluate();
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(NamedQuery.class);
		config.addConcept(Parameter.class);
		config.addBehaviour(NamedQuerySupport.class);
		super.setUp();
	}

	public void testGET_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/metadata#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/metadata#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/rdf+xml");
		Model result = evaluate.get(Model.class);
		assertFalse(result.isEmpty());
	}

	public void testPUT_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/metadata#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/metadata#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/rdf+xml");
		Model result = evaluate.get(Model.class);
		try {
			root.queryParam("evaluate", "").type("application/rdf+xml").put(
					result);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(405, e.getResponse().getStatus());
		}
	}
}
