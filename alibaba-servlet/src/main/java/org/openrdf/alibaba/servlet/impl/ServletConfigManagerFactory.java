package org.openrdf.alibaba.servlet.impl;

import info.aduna.platform.Platform;
import info.aduna.platform.PlatformFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

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
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.realiser.StatementRealiserRepository;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletConfigManagerFactory implements ElmoManagerFactory {
	private final Logger logger = LoggerFactory
			.getLogger(ServletConfigManagerFactory.class);

	private Repository repository;

	private SesameManagerFactory factory;

	public void init(ServletConfig config) throws ServletException {
		try {
			String appId = config.getInitParameter("applicationId");
			String dataDir = config.getInitParameter("dataDir");
			String id = config.getInitParameter("repositoryId");
			String initData = config.getInitParameter("initData");
			Repository repository = getRepository(getDataDir(dataDir, appId),
					id);
			repository = new StatementRealiserRepository(repository);
			initRepository(repository, initData);
			factory = new SesameManagerFactory(repository);
		} catch (Exception e) {
			ServletException exc = new ServletException(e);
			exc.initCause(e);
			throw exc;
		}
	}

	public void close() {
		if (factory != null) {
			factory.close();
		}
		try {
			if (repository != null) {
				repository.shutDown();
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
		RepositoryManager manager = new LocalRepositoryManager(dataDir);
		manager.initialize();
		Repository repository = manager.getRepository(id);
		if (repository == null) {
			logger.warn("Creating repository configuration for: {}", id);
			MemoryStoreConfig memConfig = new MemoryStoreConfig();
			SailRepositoryConfig sailConfig = new SailRepositoryConfig(
					memConfig);
			RepositoryConfig config = new RepositoryConfig(id, sailConfig);
			Repository system = manager.getSystemRepository();
			RepositoryConfigUtil.updateRepositoryConfigs(system, config);
			repository = manager.getRepository(id);
		}
		return repository;
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
