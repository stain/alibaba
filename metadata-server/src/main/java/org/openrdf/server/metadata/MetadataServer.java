package org.openrdf.server.metadata;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.openrdf.repository.Repository;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;

public class MetadataServer extends Application {

	private static final int DEFAULT_PORT = 8080;

	public static void main(String[] args) throws IOException,
			InterruptedException, StoreConfigException, StoreException {
		int port = DEFAULT_PORT;
		File dataDir = null;
		ObjectRepository repository = null;
		for (int i = 0; i + 1 < args.length; i += 2) {
			if ("-p".equals(args[i])) {
				port = Integer.parseInt(args[i + 1]);
			} else if ("-r".equals(args[i])) {
				Repository repo = RepositoryProvider.getRepository(args[i + 1]);
				if (repo instanceof ObjectRepository) {
					repository = (ObjectRepository) repo;
				} else {
					throw new IllegalArgumentException("Repository must be an ObjectRepository");
				}
			} else if ("-d".equals(args[i])) {
				dataDir = new File(args[i + 1]);
			}
		}
		MetadataServer server = new MetadataServer(repository, dataDir);
		server.setPort(port);
		server.start();
		System.out.println("Jersey app started at http://localhost:" + port);
		server.join();
		server.stop();
		System.exit(0);
	}

	private SelectorThread server;
	private int port = DEFAULT_PORT;
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

	public void start() throws IOException, StoreConfigException,
			StoreException {
		System.out.println("Starting grizzly...");
		RuntimeDelegate instance = RuntimeDelegate.getInstance();
		Adapter adapter = instance.createEndpoint(this, Adapter.class);
		String listen = "http://localhost:" + port + "/";
		server = GrizzlyServerFactory.create(listen, adapter);
	}

	public void join() throws InterruptedException {
		server.join();
	}

	public void stop() throws StoreException {
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
		providers.addAll(new MessageProviderFactory().getAll());
		return providers;
	}

}
