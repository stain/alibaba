package org.openrdf.server.metadata;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.openrdf.repository.Repository;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.store.StoreConfigException;
import org.openrdf.store.StoreException;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;

public class MetadataServer extends Application {

	private static final int DEFAULT_PORT = 8080;

	public static void main(String[] args) throws IOException,
			InterruptedException, StoreException {
		try {
			int port = DEFAULT_PORT;
			File dataDir = null;
			Repository repository = null;
			List<URL> imports = new ArrayList<URL>();
			for (int i = 0; i + 1 < args.length; i += 2) {
				if ("-p".equals(args[i])) {
					port = Integer.parseInt(args[i + 1]);
				} else if ("-r".equals(args[i])) {
					repository = RepositoryProvider.getRepository(args[i + 1]);
				} else if ("-d".equals(args[i])) {
					dataDir = new File(args[i + 1]);
				} else if ("-i".equals(args[i])) {
					imports.add(getURL(args[i + 1]));
				}
			}
			if (repository == null) {
				System.err
						.println(" -r ${repository-url} [-d ${directoryPath}] [-p ${port}]");
			} else {
				ObjectRepository or;
				if (imports.isEmpty() && repository instanceof ObjectRepository) {
					or = (ObjectRepository) repository;
				} else {
					ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
					ObjectRepositoryConfig config = factory.getConfig();
					for (URL url : imports) {
						config.addImports(url);
					}
					or = factory.createRepository(config, repository);
				}
				if (dataDir == null && repository.getDataDir() != null) {
					dataDir = new File(repository.getDataDir(), "webapp");
				} else if (dataDir == null) {
					dataDir = new File(".");
				}
				MetadataServer server = new MetadataServer(or, dataDir);
				server.setPort(port);
				server.start();
				System.out.println(server.getClass().getSimpleName()
						+ " listening on port " + port);
				System.out.println("repository: " + server.getRepository());
				System.out.println("data dir: " + server.getDataDir());
				server.join();
				server.stop();
				System.exit(0);
			}
		} catch (StoreConfigException e) {
			System.err.println(e.getMessage());
		}
	}

	private static URL getURL(String path) throws MalformedURLException {
		if (path.startsWith("http:") || path.startsWith("https:"))
			return new URL(path);
		return new File(".").toURI().resolve(path).toURL();
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

	private File getDataDir() {
		return dataDir;
	}

	private Repository getRepository() {
		return repository;
	}

	public void start() throws IOException, StoreConfigException,
			StoreException {
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
