package org.openrdf.server.metadata;

import java.util.Arrays;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class MetaResourceTest extends MetadataServerTestCase {

	public void testGET404() throws Exception {
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		Model model = graph.get(Model.class);
		assertTrue(model.isEmpty());
	}

	public void testPUT() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}

	public void testDELETE() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		graph.delete();
		model = graph.get(Model.class);
		assertTrue(model.isEmpty());
	}

	public void testGETResource() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		Model result = root.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}

	public void testGET_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/meta#inSparql");
		Literal obj = vf.createLiteral("SELECT * WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/meta#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/sparql-results+xml");
		TupleQueryResult result = evaluate.get(TupleQueryResult.class);
		assertEquals(Arrays.asList("s", "p", "o"), result.getBindingNames());
	}

	public void testPUT_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf
				.createURI("http://www.openrdf.org/rdf/2009/meta#inSparql");
		Literal obj = vf.createLiteral("SELECT * WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf
				.createURI("http://www.openrdf.org/rdf/2009/meta#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept(
				"application/sparql-results+xml");
		TupleQueryResult result = evaluate.get(TupleQueryResult.class);
		try {
			root.queryParam("evaluate", "").type(
					"application/sparql-results+xml").put(result);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(405, e.getResponse().getStatus());
		}
	}

	public void testPUTNamespace() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.setNamespace("test", "urn:test:");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph").queryParam("named-graph", "");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals("urn:test:", result.getNamespaces().get("test"));
	}
}
