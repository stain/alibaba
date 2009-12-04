package org.openrdf.server.metadata;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.server.metadata.base.MetadataServerTestCase;
import org.openrdf.server.metadata.behaviours.NamedGraphSupport;
import org.openrdf.server.metadata.behaviours.PUTSupport;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public class NamedGraphTest extends MetadataServerTestCase {

	public void setUp() throws Exception {
		config.addBehaviour(PUTSupport.class);
		config.addBehaviour(NamedGraphSupport.class, "urn:mimetype:application/rdf+xml");
		config.addBehaviour(NamedGraphSupport.class, "urn:mimetype:application/x-turtle");
		super.setUp();
	}

	public void testGET404() throws Exception {
		WebResource graph = client.path("graph");
		try {
			graph.get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testPUT() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
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
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		graph.delete();
		try {
			graph.get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testGETResource() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Model result = root.accept("application/rdf+xml").get(Model.class);
		assertTrue(result.containsAll(model));
	}

	public void testPUTNamespace() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.setNamespace("test", "urn:test:");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals("urn:test:", result.getNamespaces().get("test"));
	}

	public void testGZip() throws Exception {
		WebResource client = Client.create().resource("http://" + host);
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph");
		graph.addFilter(new GZIPContentEncodingFilter(true));
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}
}
