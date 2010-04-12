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
package org.openrdf.sail.optimistic;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencySailException;

/**
 * Relays transaction state an {@link OptimisticConnection}.
 * 
 * @author James Leigh
 *
 */
public class AutoCommitRepositoryConnection extends SailRepositoryConnection {
	private boolean active;
	private OptimisticConnection sail;

	protected AutoCommitRepositoryConnection(SailRepository repository,
			OptimisticConnection sail) {
		super(repository, sail);
		this.sail = sail;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		autoBegin();
		super.addWithoutCommit(subject, predicate, object, contexts);
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		autoBegin();
		super.removeWithoutCommit(subject, predicate, object, contexts);
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		autoBegin();
		super.clear(contexts);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		autoBegin();
		super.clearNamespaces();
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		autoBegin();
		super.removeNamespace(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name)
			throws RepositoryException {
		autoBegin();
		super.setNamespace(prefix, name);
	}

	@Override
	public void close() throws RepositoryException {
		if (active) {
			try {
				sail.rollback();
			} catch (SailException e) {
				throw new RepositoryException(e);
			}
		}
		super.close();
	}

	@Override
	public void commit() throws RepositoryException {
		try {
			sail.commit();
			if (active) {
				sail.begin();
			}
		} catch (ConcurrencySailException e) {
			throw new ConcurrencyException(e);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		super.rollback();
		try {
			if (active) {
				sail.begin();
			}
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (isAutoCommit() != autoCommit) {
			super.setAutoCommit(autoCommit);
			if (autoCommit) {
				active = false;
				try {
					sail.commit();
				} catch (SailException e) {
					throw new RepositoryException(e);
				}
			} else {
				active = true;
				try {
					sail.begin();
				} catch (SailException e) {
					throw new RepositoryException(e);
				}
			}
		}
	}

	@Override
	protected void autoCommit() throws RepositoryException {
		if (isAutoCommit()) {
			active = false;
			try {
				sail.commit();
			} catch (SailException e) {
				throw new RepositoryException(e);
			}
		}
	}

	private void autoBegin() throws RepositoryException {
		if (isAutoCommit() && !active) {
			active = true;
			try {
				sail.begin();
			} catch (SailException e) {
				throw new RepositoryException(e);
			}
		}
	}

}
