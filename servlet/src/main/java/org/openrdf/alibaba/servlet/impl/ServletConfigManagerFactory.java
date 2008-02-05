package org.openrdf.alibaba.servlet.impl;

import info.aduna.platform.Platform;
import info.aduna.platform.PlatformFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoManagerFactory;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigUtil;
import org.openrdf.repository.flushable.FlushableRepository;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes an embeded {@link SesameManagerFactory} based on a
 * {@link ServletConfig}.
 * 
 * @author James Leigh
 * 
 */
public class ServletConfigManagerFactory implements ElmoManagerFactory {
	private final Logger logger = LoggerFactory
			.getLogger(ServletConfigManagerFactory.class);

	private static ConcurrentMap<File, LocalRepositoryManager> managers = new ConcurrentHashMap<File, LocalRepositoryManager>();

	private Repository repository;

	private SesameManagerFactory factory;

	private RepositoryManager manager;

	public void init(ServletConfig config) throws ServletException {
		try {
			String appId = config.getInitParameter("applicationId");
			String dataDir = config.getInitParameter("dataDir");
			String id = config.getInitParameter("repositoryId");
			String initData = config.getInitParameter("initData");
			repository = getRepository(getDataDir(dataDir, appId), id);
			repository = new FlushableRepository(repository);
			initRepository(repository, initData);
			factory = new SesameManagerFactory(repository);
		} catch (Exception e) {
			ServletException exc = new ServletException(e);
			exc.initCause(e);
			throw exc;
		}
	}

	public Repository getRepository() {
		return repository;
	}

	public void close() {
		if (factory != null) {
			factory.close();
		}
		try {
			if (repository != null) {
				repository.shutDown();
			}
			if (manager != null) {
				manager.shutDown();
			}
		} catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public ElmoManager createElmoManager() {
		return factory.createElmoManager();
	}

	public ElmoManager createElmoManager(Locale locale) {
		return factory.createElmoManager(locale);
	}

	public boolean isOpen() {
		return factory.isOpen();
	}

	private Repository getRepository(File dataDir, String id)
			throws RepositoryException, RepositoryConfigException {
		logger.info("Using data dir: {}", dataDir);
		assert id != null;
		manager = findRepositoryManager(dataDir);
		Repository repository = manager.getRepository(id);
		if (repository == null) {
			logger.warn("Creating repository configuration for: {}", id);
			MemoryStoreConfig memConfig = new MemoryStoreConfig();
			memConfig.setPersist(true);
			SailRepositoryConfig sailConfig;
			sailConfig = new SailRepositoryConfig(memConfig);
			RepositoryConfig config = new RepositoryConfig(id, sailConfig);
			Repository system = manager.getSystemRepository();
			RepositoryConfigUtil.updateRepositoryConfigs(system, config);
			repository = manager.getRepository(id);
		}
		return repository;
	}

	private RepositoryManager findRepositoryManager(File dataDir)
			throws RepositoryException {
		LocalRepositoryManager manager = managers.get(dataDir);
		if (manager == null) {
			manager = new LocalRepositoryManager(dataDir);
			manager.initialize();
			LocalRepositoryManager o = managers.putIfAbsent(dataDir, manager);
			if (o == null)
				return manager;
			manager.shutDown();
			return o;
		}
		return manager;
	}

	private void initRepository(Repository repository, String initData)
			throws RepositoryException, MalformedURLException, IOException,
			RDFParseException {
		RepositoryConnection conn = repository.getConnection();
		try {
			if (!conn.isEmpty() || initData == null)
				return;
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			for (String file : initData.split(";")) {
				URL url;
				file = file.trim();
				if (file.length() > 0) {
					if (new File(file).exists()) {
						url = new File(file).toURI().toURL();
					} else {
						url = cl.getResource(file);
					}
					RDFFormat format = RDFFormat.forFileName(url.getFile());
					conn.add(url, "", format);
				}
			}
		} finally {
			conn.close();
		}
	}

	private File getDataDir(String dataDir, String appId) {
		if (dataDir == null) {
			assert appId != null;
			Platform platform = PlatformFactory.getPlatform();
			return platform.getApplicationDataDir(appId);
		}
		return new File(dataDir);
	}

}
