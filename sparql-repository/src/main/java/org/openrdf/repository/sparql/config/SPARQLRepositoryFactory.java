package org.openrdf.repository.sparql.config;

import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.http.config.HTTPRepositoryConfig;

/**
 * @author James Leigh
 */
public class SPARQLRepositoryFactory implements RepositoryFactory {

	public static final String REPOSITORY_TYPE = "openrdf:SPARQLRepository";

	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	public RepositoryImplConfig getConfig() {
		return new SPARQLRepositoryConfig();
	}

	public Repository getRepository(RepositoryImplConfig config)
			throws RepositoryConfigException {
		HTTPRepository result = null;

		if (config instanceof HTTPRepositoryConfig) {
			HTTPRepositoryConfig httpConfig = (HTTPRepositoryConfig) config;
			result = new HTTPRepository(httpConfig.getURL());
		} else {
			throw new RepositoryConfigException("Invalid configuration class: "
					+ config.getClass());
		}
		return result;
	}
}
