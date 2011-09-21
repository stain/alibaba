/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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

package org.openrdf.repository.query;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.RepositoryConnectionListener;
import org.openrdf.repository.event.RepositoryListener;
import org.openrdf.repository.event.base.RepositoryConnectionListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository wrapper for named query support
 * 
 * @author Steve Battle
 * 
 */

public class NamedQueryRepositoryWrapper extends RepositoryWrapper implements
		NamedQueryRepository, RepositoryListener {

	private final Logger logger = LoggerFactory
			.getLogger(NamedQueryRepositoryWrapper.class);
	private NotifyingRepository delegate;
	private Map<URI, PersistentNamedQuery> namedQueries = new HashMap<URI, PersistentNamedQuery>();

	class RepositoryConnectionMonitor extends
			RepositoryConnectionListenerAdapter implements
			RepositoryConnectionListener {
		// By default, a RepositoryConnection is in autoCommit mode.
		boolean autoCommit = true, updatePending = false;

		private void commit() {
			if (updatePending)
				update(System.currentTimeMillis());
			updatePending = false;
		}

		@Override
		public void add(RepositoryConnection conn, Resource subject,
				URI predicate, Value object, Resource... contexts) {
			updatePending = true;
			if (autoCommit)
				commit();
		}

		@Override
		public void clear(RepositoryConnection conn, Resource... contexts) {
			updatePending = true;
			if (autoCommit)
				commit();
		}

		@Override
		public void commit(RepositoryConnection conn) {
			commit();
		}

		@Override
		public void remove(RepositoryConnection conn, Resource subject,
				URI predicate, Value object, Resource... contexts) {
			updatePending = true;
			if (autoCommit)
				commit();
		}

		@Override
		public void rollback(RepositoryConnection conn) {
			updatePending = false;
		}

		@Override
		public void setAutoCommit(RepositoryConnection con, boolean autoCommit) {
			this.autoCommit = autoCommit;
			if (autoCommit)
				commit();
		}

		@Override
		public void close(RepositoryConnection conn) {
			delegate.removeRepositoryConnectionListener(this);
		}

	}

	public NamedQueryRepositoryWrapper() {
		super();
	}

	/**
	 * The constructor requires an immediate delegate that is a notifying
	 * repository
	 */

	public NamedQueryRepositoryWrapper(NotifyingRepository delegate) {
		super(delegate);
		// keep a local reference to the delegate
		this.delegate = delegate;
		if (delegate != null)
			delegate.addRepositoryListener(this);
	}

	/*
	 * The NamedQueryRepository depends on a NotifyingRepository to listen to.
	 * The delegate may be an instance of NotifyingRepository, or it may wrap
	 * one If there is no NotifyingRepository in the chain the delegate is null
	 */

	@Override
	public void setDelegate(Repository delegate) {
		super.setDelegate(delegate);

		// search the delegate chain for a suitable NotifyingRepository
		this.delegate = getNotifyingDelegate(delegate);

		if (this.delegate != null)
			this.delegate.addRepositoryListener(this);
	}

	/*
	 * Support for the NamedQueryRepository interface A URI mapping may be
	 * overwritten
	 */

	public synchronized NamedQuery createNamedQuery(URI uri, QueryLanguage ql,
			String queryString, String baseURI) throws RepositoryException {
		PersistentNamedQuery nq;
		nq = new PersistentNamedQuery(uri, ql, queryString, baseURI);
		namedQueries.put(uri, nq);
		return nq;
	}

	public synchronized void removeNamedQuery(URI uri) {
		namedQueries.remove(uri);
	}

	public synchronized URI[] getNamedQueryIDs() {
		Set<URI> uris = namedQueries.keySet();
		return uris.toArray(new URI[uris.size()]);
	}

	public synchronized NamedQuery getNamedQuery(URI uri) {
		return namedQueries.get(uri);
	}

	/* Methods for Repository Listener */

	public void getConnection(Repository repo, RepositoryConnection con) {
		delegate
				.addRepositoryConnectionListener(new RepositoryConnectionMonitor());
	}

	/* persist (rehydrate) the named queries on startup */

	public synchronized void initialize(Repository repo) {
		File dataDir = delegate.getDataDir();
		if (dataDir != null && dataDir.isDirectory())
			try {
				namedQueries = PersistentNamedQuery.persist(dataDir);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
	}

	public void setDataDir(Repository repo, File dataDir) {
	}

	/* desist (dehydrate) the named queries on shutdown */

	public synchronized void shutDown(Repository repo) {
		delegate.removeRepositoryListener(this);

		// desist all named queries
		File dataDir = delegate.getDataDir();
		if (dataDir != null && dataDir.isDirectory()) {
			try {
				PersistentNamedQuery.desist(dataDir, namedQueries);
			} catch (RepositoryException e) {
				logger.error(e.toString(), e);
			}
		}
	}

	/* search the delegate chain for a notifying repository */

	private static NotifyingRepository getNotifyingDelegate(Repository delegate) {
		while (delegate != null) {
			if (delegate instanceof NotifyingRepository) {
				return (NotifyingRepository) delegate;
			} else if (delegate instanceof RepositoryWrapper) {
				delegate = ((RepositoryWrapper) delegate).getDelegate();
			} else
				break;
		}
		return null;
	}

	private synchronized void update(long time) {
		URI[] uris = this.getNamedQueryIDs();
		for (int i = 0; i < uris.length; i++) {
			namedQueries.get(uris[i]).update(time);
		}
	}

}
