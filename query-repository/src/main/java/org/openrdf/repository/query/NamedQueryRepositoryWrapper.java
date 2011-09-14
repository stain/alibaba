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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UnsupportedQueryLanguageException;
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

public class NamedQueryRepositoryWrapper extends RepositoryWrapper implements NamedQueryRepository, RepositoryListener {

	private final Logger logger = LoggerFactory.getLogger(NamedQueryRepositoryWrapper.class) ;	
	NotifyingRepository delegate ;
	Map<URI,PersistentNamedQueryImpl> namedQueries = new HashMap<URI,PersistentNamedQueryImpl>() ;
	
	class RepositoryConnectionMonitor extends RepositoryConnectionListenerAdapter implements RepositoryConnectionListener {
		// By default, a RepositoryConnection is in autoCommit mode.
		boolean autoCommit = true, updatePending = false;
		
		private void commit() {
			if (updatePending) update() ;
			updatePending = false ;
		}

		@Override
		public void add(RepositoryConnection conn, Resource subject, URI predicate, Value object, Resource... contexts) {
			updatePending = true ;
			if (autoCommit) commit() ;
		}

		@Override
		public void clear(RepositoryConnection conn, Resource... contexts) {
			updatePending = true ;
			if (autoCommit) commit() ;
		}

		@Override
		public void commit(RepositoryConnection conn) {
			commit() ; 		
		}

		@Override
		public void remove(RepositoryConnection conn, Resource subject, URI predicate, Value object, Resource... contexts) {
			updatePending = true ;
			if (autoCommit) commit() ;
		}

		@Override
		public void rollback(RepositoryConnection conn) {
			updatePending = false ;			
		}
		
		@Override
		public void setAutoCommit(RepositoryConnection con, boolean autoCommit) {
			this.autoCommit = autoCommit ;
			if (autoCommit) commit() ;
		}
		
		@Override
		public void close(RepositoryConnection conn) {
			delegate.removeRepositoryConnectionListener(this) ;
		}
		
	}
	
	void update() {
		long time = System.currentTimeMillis() ;
		for (Iterator<URI> uris = this.getNamedQueryURIs(); uris.hasNext(); ) {
			namedQueries.get(uris.next()).update(time) ;
		}
	}
	
	/* constructors */

	public NamedQueryRepositoryWrapper() {
		super();
	}

	public NamedQueryRepositoryWrapper(NotifyingRepository delegate) {
		super(delegate);
		// keep a local reference to the delegate
		this.delegate = delegate ;
		delegate.addRepositoryListener(this) ;
	}
	
	/* Support for the NamedQueryRepository interface 
	 * A URI mapping may be overwritten
	 */

	public NamedQuery createNamedQuery(URI uri, QueryLanguage ql, String queryString, String baseURI) 
	throws MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		PersistentNamedQueryImpl nq ;
		nq = new PersistentNamedQueryImpl(uri, ql, queryString, baseURI) ;
		namedQueries.put(uri, nq) ;
		return nq ;	
	}

	public synchronized void removeNamedQuery(URI uri) {
		PersistentNamedQueryImpl nq = namedQueries.get(uri) ;
		File dataDir = getDataDir() ;
		if (dataDir!=null && nq!=null) {
			nq.cease(PersistentNamedQueryImpl.getDataDir(dataDir)) ;
		}
		namedQueries.remove(uri) ;
	}

	public synchronized Iterator<URI> getNamedQueryURIs() {
		return namedQueries.keySet().iterator();
	}

	public synchronized NamedQuery getNamedQuery(URI uri) {
		return namedQueries.containsKey(uri)?namedQueries.get(uri):null;		
	}
	
	/* Methods for Repository Listener */

	public void getConnection(Repository repo, RepositoryConnection con) {
		delegate.addRepositoryConnectionListener(new RepositoryConnectionMonitor()) ;
	}
	
	/* persist (rehydrate) the named queries on startup */

	public void initialize(Repository repo) {
		ValueFactory vf = getValueFactory() ;
		File dataDir = delegate.getDataDir() ;
		if (dataDir!=null && dataDir.isDirectory()) {
			Iterator<Properties> pi = PersistentNamedQueryImpl.persist(dataDir) ;
			while (pi.hasNext()) {
				try {
					PersistentNamedQueryImpl nq ;
					nq = new PersistentNamedQueryImpl(dataDir, pi.next(), vf);
					namedQueries.put(nq.getUri(), nq) ;
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}
 	}

	public void setDataDir(Repository repo, File dataDir) {}
	
	/* desist (dehydrate) the named queries on shutdown */

	public void shutDown(Repository repo) {
		delegate.removeRepositoryListener(this) ;
		
		// desist all named queries
		File dataDir = delegate.getDataDir() ;
		if (dataDir!=null && dataDir.isDirectory()) {
			File dir = PersistentNamedQueryImpl.getDataDir(dataDir) ;
			for (PersistentNamedQueryImpl nq: namedQueries.values()) {
				try {
					nq.desist(dir) ;
				} catch (RepositoryException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}


}
