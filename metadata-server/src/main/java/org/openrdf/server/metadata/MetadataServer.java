package org.openrdf.server.metadata;

import java.io.File;
import java.io.IOException;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectRepository;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.grizzly.standalone.StaticStreamAlgorithm;

public class MetadataServer {
	private SelectorThread server;
	private ObjectRepository repository;
	private File dataDir;
	private MetadataServlet servlet;

	public MetadataServer(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
		server = new SelectorThread();
		server.setAlgorithmClassName(StaticStreamAlgorithm.class.getName());
		servlet = new MetadataServlet(repository, dataDir);
		ServletAdapter adapter = new ServletAdapter();
		adapter.setServletInstance(servlet);
		server.setAdapter(adapter);
	}

	public int getPort() {
		return server.getPort();
	}

	public void setPort(int port) {
		server.setPort(port);
	}

	public File getDataDir() {
		return dataDir;
	}

	public Repository getRepository() {
		return repository;
	}

	public String getServerName() {
		return servlet.getServerName();
	}

	public void setServerName(String serverName) {
		servlet.setServerName(serverName);
	}

	public void start() throws IOException, RepositoryConfigException,
			RepositoryException {
		try {
			server.listen();
		} catch (InstantiationException e) {
		    IOException _e = new IOException();
		    _e.initCause(e);
		    throw _e;
		}
	}

	public void join() throws InterruptedException {
		server.join();
	}

	public void stop() throws RepositoryException {
		server.stopEndpoint();
	}

}
