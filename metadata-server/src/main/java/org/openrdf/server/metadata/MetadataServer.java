package org.openrdf.server.metadata;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.openrdf.repository.Repository;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.store.StoreException;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.tcp.Adapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;

public class MetadataServer extends Application {

	public static void main(String[] args) throws IOException,
			InterruptedException, StoreException {
		MetadataServer server = new MetadataServer();
		for (int i = 0; i + 1 < args.length; i += 2) {
			if ("-p".equals(args[i])) {
				server.setPort(Integer.parseInt(args[i + 1]));
			} else if ("-r".equals(args[i])) {
				server.setRepository(args[i + 1]);
			}
		}
		server.start();
		int port = server.getPort();
		System.out.println("Jersey app started at http://localhost:" + port);
		server.join();
		server.stop();
		System.exit(0);
	}

	private SelectorThread server;
	private int port = 8080;
	private String url;
	private Repository repository;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getRepository() {
		return url;
	}

	public void setRepository(String url) {
		this.url = url;
	}

	public void start() throws IOException, StoreException {
		repository = new HTTPRepository(url);
		repository.initialize();
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
		if (repository != null) {
			repository.shutDown();
			repository = null;
		}
	}

	@Override
	public Set<Class<?>> getClasses() {
		return new HashSet<Class<?>>(0);
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> providers = new HashSet<Object>();
		providers.add(new MetaDataResource(repository));
		providers.add(new GeneralExceptionMapper());
		providers.addAll(new MessageProviderFactory().getAll());
		return providers;
	}

}
