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
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.result.TupleResult;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
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

	public void testGETExecute() throws Exception {
		Model model = new LinkedHashModel();
		WebResource root = client.path("root");
		URI subj = vf.createURI(root.getURI().toASCIIString());
		URI pred = vf.createURI("http://www.openrdf.org/rdf/2009/04/metadata#inSparql");
		Literal obj = vf.createLiteral("SELECT * WHERE { ?s ?p ?o }");
		model.add(subj, RDF.TYPE, vf.createURI("http://www.openrdf.org/rdf/2009/04/metadata#Query"));
		model.add(subj, pred, obj);
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		TupleResult result = root.queryParam("execute", "").get(TupleResult.class);
		assertEquals(Arrays.asList("s", "p", "o"), result.getBindingNames());
	}

	private ObjectRepository createRepository() throws StoreException,
			StoreConfigException {
		ObjectRepository repo;
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		repo = factory.createRepository(new SailRepository(new MemoryStore()));
		repo.initialize();
		return repo;
	}
}