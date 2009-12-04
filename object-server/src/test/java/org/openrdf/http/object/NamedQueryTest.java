package org.openrdf.http.object;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.title;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.NamedGraphSupport;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.iri;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class NamedQueryTest extends MetadataServerTestCase {

	@iri("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery")
	public interface NamedQuery {

		@iri("http://www.openrdf.org/rdf/2009/httpobject#inSparql")
		String getMetaInSparql();

		void setMetaInSparql(String sparql);

		@iri("http://www.openrdf.org/rdf/2009/httpobject#parameter")
		Set<Parameter> getMetaParameters();

		void setMetaParameters(Set<Parameter> parameters);
	}

	@iri("http://www.openrdf.org/rdf/2009/httpobject#Parameter")
	public interface Parameter {

		@iri("http://www.openrdf.org/rdf/2009/httpobject#name")
		String getMetaName();
		void setMetaName(String name);

		@iri("http://www.openrdf.org/rdf/2009/httpobject#base")
		Object getMetaBase();
		void setMetaBase(Object base);

		@iri("http://www.openrdf.org/rdf/2009/httpobject#datatype")
		Object getMetaDatatype();
		void setMetaDatatype(Object datatype);

	}

	public static abstract class NamedQuerySupport implements NamedQuery, RDFObject {
		public static int switched;
		public static int evaluated;

		@title("Evaluate Query")
		@operation("evaluate")
		public URL evaluate(@parameter("*") Map<String, String[]> parameters)
				throws RepositoryException, MalformedQueryException,
				MalformedURLException, UnsupportedEncodingException {
			switched++;
			Query query = createQuery();
			String operation;
			if (query instanceof GraphQuery) {
				operation = "evaluateGraphQuery";
			} else if (query instanceof TupleQuery) {
				operation = "evaluateTupleQuery";
			} else if (query instanceof BooleanQuery) {
				operation = "evaluateBooleanQuery";
			} else {
				throw new IllegalArgumentException();
			}
			StringBuilder sb = new StringBuilder();
			sb.append(getResource().stringValue());
			sb.append("?").append(operation);
			for (String name : parameters.keySet()) {
				for (String value : parameters.get(name)) {
					if (value == null || value.length() < 1)
						continue;
					sb.append("&").append(URLEncoder.encode(name, "UTF-8"));
					sb.append("=").append(URLEncoder.encode(value, "UTF-8"));
				}
			}
			return new URL(sb.toString());
		}

		@cacheControl("must-reevaluate")
		@operation("evaluateGraphQuery")
		public GraphQueryResult evaluateGraphQuery(@parameter("*") Map<String, String[]> parameters)
				throws RepositoryException, URISyntaxException,
				QueryEvaluationException, MalformedQueryException {
			evaluated++;
			GraphQuery query = (GraphQuery)prepareQuery(createQuery(), parameters);
			return query.evaluate();
		}

		@cacheControl("must-reevaluate")
		@operation("evaluateTupleQuery")
		public TupleQueryResult evaluateTupleQuery(@parameter("*") Map<String, String[]> parameters)
				throws RepositoryException, URISyntaxException,
				QueryEvaluationException, MalformedQueryException {
			evaluated++;
			TupleQuery query = (TupleQuery)prepareQuery(createQuery(), parameters);
			return query.evaluate();
		}

		@cacheControl("must-reevaluate")
		@operation("evaluateBooleanQuery")
		public boolean evaluateBooleanQuery(@parameter("*") Map<String, String[]> parameters)
				throws RepositoryException, URISyntaxException,
				QueryEvaluationException, MalformedQueryException {
			evaluated++;
			BooleanQuery query = (BooleanQuery)prepareQuery(createQuery(), parameters);
			return query.evaluate();
		}

		private Query createQuery() throws RepositoryException,
				MalformedQueryException {
			ObjectConnection con = getObjectConnection();
			return con.prepareQuery(SPARQL, getMetaInSparql());
		}

		private Query prepareQuery(Query query, Map<String, String[]> parameters)
				throws RepositoryException, MalformedQueryException,
				URISyntaxException {
			ValueFactory vf = getObjectConnection().getValueFactory();
			for (Parameter parameter : getMetaParameters()) {
				String name = parameter.getMetaName();
				String[] values = parameters.get(name);
				if (values == null || values.length < 1 || values[0].length() < 1)
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
			return query;
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(NamedQuery.class);
		config.addConcept(Parameter.class);
		config.addBehaviour(NamedQuerySupport.class);
		config.addBehaviour(PUTSupport.class);
		config.addBehaviour(NamedGraphSupport.class);
		NamedQuerySupport.switched = 0;
		NamedQuerySupport.evaluated = 0;
		super.setUp();
	}

	public void testGET_evaluateGraph() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/rdf+xml");
		Model result = evaluate.get(Model.class);
		assertFalse(result.isEmpty());
	}

	public void testReevaluateGraph() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/rdf+xml");
		Model result = evaluate.get(Model.class);
		Thread.sleep(1000);
		evaluate = evaluate.accept("application/rdf+xml");
		model.clear();
		URI uri = vf.createURI("urn:root");
		model.add(uri, uri, uri);
		client.path("/thing").type("application/x-turtle").put(model);
		assertFalse(result.equals(evaluate.get(Model.class)));
		assertEquals(1, NamedQuerySupport.switched);
		assertEquals(2, NamedQuerySupport.evaluated);
	}

	public void testGET_evaluateTuple() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf
				.createLiteral("SELECT ?s ?p ?o WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/sparql-results+xml");
		String result = evaluate.get(String.class);
		assertTrue(result.startsWith("<?xml"));
	}

	public void testPUT_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#inSparql");
		Literal obj = vf
				.createLiteral("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/httpobject#NamedQuery"));
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
