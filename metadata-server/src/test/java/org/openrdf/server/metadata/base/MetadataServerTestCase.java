package org.openrdf.server.metadata.base;

import info.aduna.io.FileUtil;

import java.io.File;
import java.net.BindException;

import junit.framework.TestCase;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.sail.Sail;
import org.openrdf.sail.auditing.AuditingSail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.optimistic.OptimisticRepository;
import org.openrdf.server.metadata.MetadataServer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public abstract class MetadataServerTestCase extends TestCase {
	private static volatile int port = 3128;
	protected ObjectRepository repository;
	protected ObjectRepositoryConfig config = new ObjectRepositoryConfig();
	private MetadataServer server;
	protected File dataDir;
	protected String host;
	protected WebResource client;
	protected ValueFactory vf;
	protected String base;

	@Override
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		dataDir = FileUtil.createTempDir("metadata");
		server = new MetadataServer(repository, dataDir, new File(dataDir, "cache"));
		while (true) {
			try {
				server.setPort(port++);
				server.start();
				break;
			} catch (BindException e) {
				continue;
			}
		}
		host = "localhost:" + server.getPort();
		client = Client.create().resource("http://" + host);
		client.addFilter(new GZIPContentEncodingFilter());
		base = client.getURI().toASCIIString();
	}

	@Override
	public void tearDown() throws Exception {
		server.stop();
		repository.shutDown();
		FileUtil.deltree(dataDir);
	}

	@Override
	protected void runTest() throws Throwable {
		try {
			super.runTest();
		} catch (UniformInterfaceException e) {
			System.out.println(e.getResponse().getEntity(String.class));
			throw e;
		}
	}

	private ObjectRepository createRepository() throws Exception {
		Sail sail = new MemoryStore();
		sail = new AuditingSail(sail);
		Repository repo = new OptimisticRepository(sail);
		repo.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(config, repo);
	}

}
