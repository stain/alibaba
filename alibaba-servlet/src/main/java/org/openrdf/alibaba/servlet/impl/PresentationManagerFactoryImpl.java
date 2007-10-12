package org.openrdf.alibaba.servlet.impl;

import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

import org.openrdf.alibaba.servlet.PresentationManager;
import org.openrdf.alibaba.servlet.PresentationManagerFactory;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

public class PresentationManagerFactoryImpl implements
		PresentationManagerFactory {

	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String ALI_PREFIX = "ali";

	private Repository repository;

	private SesameManagerFactory factory;

	public void initialize() {
		repository = new SailRepository(new MemoryStore());
		try {
			repository.initialize();
			initMetaData(repository);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		factory = new SesameManagerFactory(repository);
	}

	public void close() {
		try {
			repository.shutDown();
			repository = null;
		} catch (RepositoryException e) {
			throw new AssertionError(e);
		}
	}

	public boolean isOpen() {
		return repository != null;
	}

	public PresentationManager createManager(Locale locale) {
		return new PresentationManagerImpl(factory.createElmoManager(locale));
	}

	private void initMetaData(Repository repository) throws Exception {
		RepositoryConnection conn = repository.getConnection();
		try {
			conn.setNamespace(ALI_PREFIX, ALI.NS);
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			loadPropertyKeysAsResource(conn, cl, POVS_PROPERTIES);
			loadPropertyKeysAsResource(conn, cl, DECORS_PROPERTIES);
		} finally {
			conn.close();
		}
	}

	private void loadPropertyKeysAsResource(RepositoryConnection conn,
			ClassLoader cl, String listing) throws Exception {
		Enumeration<URL> list = cl.getResources(listing);
		while (list.hasMoreElements()) {
			Properties prop = new Properties();
			prop.load(list.nextElement().openStream());
			for (Object res : prop.keySet()) {
				URL url = cl.getResource(res.toString());
				RDFFormat format = RDFFormat.forFileName(url.getFile());
				conn.add(url, "", format);
			}
		}
	}

}
