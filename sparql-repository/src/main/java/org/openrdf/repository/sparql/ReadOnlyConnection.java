package org.openrdf.repository.sparql;

import info.aduna.iteration.Iteration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public abstract class ReadOnlyConnection extends ConnectionBase {

	public ReadOnlyConnection(Repository repository) {
		super(repository);
	}

	public void commit() throws RepositoryException {
		// no-op
	}

	public boolean isAutoCommit() throws RepositoryException {
		return false;
	}

	public void rollback() throws RepositoryException {
		// no-op
	}

	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (!autoCommit) {
			throw new UnsupportedOperationException();
		}
	}

	public void add(Statement st, Resource... contexts)
			throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void add(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		throw new UnsupportedOperationException();
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void add(File file, String baseURI, RDFFormat dataFormat,
			Resource... contexts) throws IOException, RDFParseException,
			RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void add(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void clear(Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void clearNamespaces() throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void remove(Statement st, Resource... contexts)
			throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void remove(Iterable<? extends Statement> statements,
			Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter,
			Resource... contexts) throws RepositoryException, E {
		throw new UnsupportedOperationException();
	}

	public void remove(Resource subject, URI predicate, Value object,
			Resource... contexts) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void removeNamespace(String prefix) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		throw new UnsupportedOperationException();
	}

}
