/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
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

/**
 * Throws {@link UnsupportedOperationException} for write operations.
 * 
 * @author James Leigh
 *
 */
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
