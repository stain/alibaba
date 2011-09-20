/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.sail.optimistic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.NamedQueryRepository;
import org.openrdf.repository.query.PersistentNamedQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows concurrent write connections. Optionally enforces snapshot and
 * serializable transaction isolation.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public class OptimisticRepository extends SailRepository implements NamedQueryRepository {

	private final Logger logger = LoggerFactory.getLogger(OptimisticRepository.class) ;	
	private OptimisticSail sail;
	private Map<URI,PersistentNamedQuery> namedQueries = new HashMap<URI,PersistentNamedQuery>() ;

	public OptimisticRepository(Sail sail) {
		super(new OptimisticSail(sail));
		this.sail = (OptimisticSail) getSail();
	}

	/**
	 * @return <code>true</code> if the new connections will enforce snapshot
	 *         isolation.
	 */
	public boolean isSnapshot() {
		return sail.isSnapshot();
	}

	public void setSnapshot(boolean snapshot) {
		sail.setSnapshot(snapshot);
	}

	/**
	 * @return <code>true</code> if new connections will enforce serializable
	 *         isolation.
	 */
	public boolean isSerializable() {
		return sail.isSerializable();
	}

	public void setSerializable(boolean serializable) {
		sail.setSerializable(serializable);
	}

	@Override
	public SailRepositoryConnection getConnection() throws RepositoryException {
		try {
			OptimisticConnection con = sail.getConnection();
			return new AutoCommitRepositoryConnection(this, con);
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}
	
	/* Methods supporting the NamedQueryRepository interface */

	public synchronized NamedQuery createNamedQuery(URI uri, QueryLanguage ql, String queryString, String baseURI) 
	throws RepositoryException {
		// allow existing mapping to be overwritten
		// but detach the old named query from the repository
		if (namedQueries.containsKey(uri))
			sail.removeSailChangedListener((SailChangedListener) namedQueries.get(uri));
		OptimisticNamedQueryImpl nq ;
		nq = new OptimisticNamedQueryImpl(ql, queryString, baseURI) ;
		namedQueries.put(uri, nq) ;
		sail.addSailChangedListener(nq) ;
		return nq ;
	}
	
	public synchronized NamedQuery getNamedQuery(URI uri) {
		return namedQueries.get(uri) ;
	}
	
	public synchronized URI[] getNamedQueryURIs() {
		Set<URI> uris = namedQueries.keySet() ;
		return uris.toArray(new URI[uris.size()]);
	}
	
	public synchronized void removeNamedQuery(URI uri) {
		PersistentNamedQuery nq = namedQueries.get(uri) ;
		sail.removeSailChangedListener((SailChangedListener) nq) ;
		File dataDir = getDataDir() ;
		if (dataDir!=null && nq!=null) {
			nq.cease(dataDir, uri) ;
		}
		namedQueries.remove(uri) ;
	}
	
	/* Override initialize(), shutdown() to support persistence */
	
	@Override
	public synchronized void initialize() throws RepositoryException {
		super.initialize() ;
		
		// persist stored named queries
		ValueFactory vf = getValueFactory() ;
		File dataDir = getDataDir() ;
		if (dataDir!=null && dataDir.isDirectory()) try {
			namedQueries = OptimisticNamedQueryImpl.persist(dataDir,vf) ;
		} catch (Exception e) {
			logger.warn(e.getMessage()) ;
		}
	}
	
	/* Desist all active named queries */

	@Override
	public synchronized void shutDown() throws RepositoryException {
		super.shutDown();
		
		// desist all named queries
		File dataDir = getDataDir() ;
		if (dataDir!=null && dataDir.isDirectory()) {
			for (URI uri: namedQueries.keySet()) {
				namedQueries.get(uri).desist(dataDir, uri) ;
			}
		}
	}

}
