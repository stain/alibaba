package org.openrdf.server.metadata;

import info.aduna.io.FileUtil;

import java.io.File;
import java.util.Set;

import junit.framework.TestCase;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class TransactionResourceTest extends TestCase {
	private ObjectRepositoryConfig config = new ObjectRepositoryConfig();
	private ObjectRepository repository;
	private MetadataServer server;
	private File dataDir;
	private String host;
	private WebResource client;
	private ValueFactory vf;

	public static class HelloWorld {
		@operation("operation")
		public String hello(String input) {
			return input + " world!";
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addBehaviour(HelloWorld.class, new URIImpl("urn:test:HelloWorld"));
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

	public void testPOST() throws Exception {
		WebResource path = client.path("interface");
		Model model = new LinkedHashModel();
		URI root = vf.createURI(path.getURI().toASCIIString());
		URI obj = vf.createURI("urn:test:HelloWorld");
		model.add(root, RDF.TYPE, obj);
		WebResource graph = client.path("graph").queryParam("describe", "");
		graph.type("application/x-turtle").put(model);
		assertEquals("hello world!", path.post(String.class, "hello"));
	}

	private ObjectRepository createRepository() throws StoreException,
			StoreConfigException {
		ObjectRepository repo;
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		MemoryStore sail = new MemoryStore();
		repo = factory.getRepository(config);
		repo.setDelegate(new SailRepository(sail));
		repo.initialize();
		return repo;
	}
}
