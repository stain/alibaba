package org.openrdf.sail.optimistic.config;

import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.config.SailRepositoryFactory;
import org.openrdf.sail.optimistic.OptimisticRepository;

/**
 * @author James Leigh
 */
public class OptimisticFactory extends SailRepositoryFactory {

	public static final String REPOSITORY_TYPE = "openrdf:OptimisticRepository";

	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config)
			throws RepositoryConfigException {
		SailRepository repository = (SailRepository) super.getRepository(config);
		return new OptimisticRepository(repository.getSail());
	}
}
