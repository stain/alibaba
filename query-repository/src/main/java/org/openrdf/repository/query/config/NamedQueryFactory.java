package org.openrdf.repository.query.config;

import java.io.File;
import java.util.Properties;

import org.openrdf.repository.query.NamedQueryRepository;

public interface NamedQueryFactory {

	public NamedQueryRepository.NamedQuery createNamedQuery(File dataDir, Properties props) throws Exception ;

}
