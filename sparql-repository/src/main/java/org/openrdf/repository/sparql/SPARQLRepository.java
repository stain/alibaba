package org.openrdf.repository.sparql;

import java.io.File;
import java.util.Set;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class SPARQLRepository implements Repository {
	private String url;
	private PrefixHashSet subjects;

	public SPARQLRepository(String url) {
		this.url = url;
	}

	public void setSubjectSpaces(Set<String> subjects) {
		this.subjects = new PrefixHashSet(subjects);
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		return new SPARQLConnection(this, url, subjects);
	}

	public File getDataDir() {
		return null;
	}

	public ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	public void initialize() throws RepositoryException {
		// no-op
	}

	public boolean isWritable() throws RepositoryException {
		return false;
	}

	public void setDataDir(File dataDir) {
		// no-op
	}

	public void shutDown() throws RepositoryException {
		// no-op
	}

}
