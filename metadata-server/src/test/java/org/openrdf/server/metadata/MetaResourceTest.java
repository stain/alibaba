package org.openrdf.server.metadata;

import info.aduna.io.FileUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import junit.framework.TestCase;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class MetaResourceTest extends TestCase {
	private ObjectRepository repository;
	private MetadataServer server;
	private File dataDir;
	private String host;
	private WebResource client;
	private ValueFactory vf;

	@Override
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		dataDir = FileUtil.createTempDir("metadata");
		server = new MetadataServer(repository, dataDir);
		server.setPort(3453);
		server.start();
		host = "localhost:" + server.getPort();
		ClientConfig config = new DefaultClientConfig() {
			@Override
			public Set<Object> getSingletons() {
				return new MessageProviderFactory().getAll();
			}
		};
		client = Client.create(config).resource("http://" + host);
	}

	@Override
	public void tearDown() throws Exception {
		server.stop();
		repository.shutDown();
		FileUtil.deltree(dataDir);
	}

	public void testGET404() throws Exception {
		WebResource graph = client.path("graph").queryParam("describe", "");
		Model model = graph.get(Model.class);
		assertTrue(model.isEmpty());
	}

	public void testPUT() throws Exception {
		Model model = new LinkedHashModel();
		URI root = vf.createURI("urn:test:root");
		URI pred = vf.createURI("urn:test:pred");
		URI obj = vf.createURI("urn:test:obj");
		model.add(root, pred, obj);
		WebResource graph = client.path("graph").queryParam("describe", "");
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
		WebResource graph = client.path("graph").queryParam("describe", "");
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
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		Model result = root.accept("application/rdf+xml").get(Model.class);
		assertEquals(model, result);
	}

	public void testGET_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf.createURI("http://www.openrdf.org/rdf/2009/meta#inSparql");
		Literal obj = vf.createLiteral("SELECT * WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf.createURI("http://www.openrdf.org/rdf/2009/meta#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept("application/sparql-results+xml");
		TupleQueryResult result = evaluate.get(TupleQueryResult.class);
		assertEquals(Arrays.asList("s", "p", "o"), result.getBindingNames());
	}

	public void testPUT_evaluate() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf.createURI("http://www.openrdf.org/rdf/2009/meta#inSparql");
		Literal obj = vf.createLiteral("SELECT * WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf.createURI("http://www.openrdf.org/rdf/2009/meta#NamedQuery"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		Builder evaluate = root.queryParam("evaluate", "").accept("application/sparql-results+xml");
		TupleQueryResult result = evaluate.get(TupleQueryResult.class);
		try {
			root.queryParam("evaluate", "").type("application/sparql-results+xml").put(result);
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
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		Model result = graph.accept("application/rdf+xml").get(Model.class);
		assertEquals("urn:test:", result.getNamespaces().get("test"));
	}

	private ObjectRepository createRepository() throws Exception {
		SailRepository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(repo);
	}
}
