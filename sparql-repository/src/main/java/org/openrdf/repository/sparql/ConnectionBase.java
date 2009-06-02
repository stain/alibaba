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

import info.aduna.iteration.EmptyIteration;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * Rewrites some connection into other connection calls.
 * 
 * @author James Leigh
 *
 */
public abstract class ConnectionBase implements RepositoryConnection {
	private Repository repository;
	private boolean closed;

	public ConnectionBase(Repository repository) {
		this.repository = repository;
	}

	public void close() throws RepositoryException {
		closed = true;
	}

	public void export(RDFHandler handler, Resource... contexts)
			throws RepositoryException, RDFHandlerException {
		exportStatements(null, null, null, false, handler, contexts);
	}

	public String getNamespace(String prefix) throws RepositoryException {
		return null;
	}

	public RepositoryResult<Namespace> getNamespaces()
			throws RepositoryException {
		return new RepositoryResult<Namespace>(
				new EmptyIteration<Namespace, RepositoryException>());
	}

	public Repository getRepository() {
		return repository;
	}

	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	public boolean hasStatement(Statement st, boolean inf, Resource... contexts)
			throws RepositoryException {
		return hasStatement(st.getSubject(), st.getPredicate(), st.getObject(),
				inf, contexts);
	}

	public boolean isEmpty() throws RepositoryException {
		return !hasStatement(null, null, null, false);
	}

	public boolean isOpen() throws RepositoryException {
		return !closed;
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareBooleanQuery(ql, query, "");
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareGraphQuery(ql, query, "");
	}

	public Query prepareQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareQuery(ql, query, "");
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
			throws RepositoryException, MalformedQueryException {
		return prepareTupleQuery(ql, query, "");
	}

	public long size(Resource... contexts) throws RepositoryException {
		RepositoryResult<Statement> stmts = getStatements(null, null, null,
				true, contexts);
		try {
			long i = 0;
			while (stmts.hasNext()) {
				stmts.next();
				i++;
			}
			return i;
		} finally {
			stmts.close();
		}
	}
}
