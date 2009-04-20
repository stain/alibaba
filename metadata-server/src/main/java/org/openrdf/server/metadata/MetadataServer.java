package org.openrdf.server.metadata;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectRepository;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;

public class MetadataServer extends Application {
	private SelectorThread server;
	private int port = 80;
	private ObjectRepository repository;
	private File dataDir;

	public MetadataServer(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public File getDataDir() {
		return dataDir;
	}

	public Repository getRepository() {
		return repository;
	}

	public void start() throws IOException, RepositoryConfigException,
			RepositoryException {
		RuntimeDelegate instance = RuntimeDelegate.getInstance();
		Adapter adapter = instance.createEndpoint(this, Adapter.class);
		String listen = "http://localhost:" + port + "/";
		server = GrizzlyServerFactory.create(listen, adapter);
	}

	public void join() throws InterruptedException {
		server.join();
	}

	public void stop() throws RepositoryException {
		if (server != null) {
			server.stopEndpoint();
			server = null;
		}
	}

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> resources = new HashSet<Class<?>>();
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> providers = new HashSet<Object>();
		providers.add(new MetaDataResource(repository, dataDir));
		providers.add(new GeneralExceptionMapper());
		return providers;
	}

}
