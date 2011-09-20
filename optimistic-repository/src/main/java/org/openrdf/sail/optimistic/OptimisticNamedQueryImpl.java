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

package org.openrdf.sail.optimistic;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.PersistentNamedQuery;
import org.openrdf.repository.query.PersistentNamedQueryImpl;
import org.openrdf.repository.query.PersistentNamedQueryWrapper;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailChangedEvent;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.DefaultSailChangedEvent;
import org.openrdf.sail.optimistic.helpers.BasicNodeCollector;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of named queries with optimistic update, wrapping a persistent named query
 * 
 * @author Steve Battle
 *
 */

class OptimisticNamedQueryImpl extends PersistentNamedQueryWrapper implements SailChangedListener {
	
	private final Logger logger = LoggerFactory.getLogger(OptimisticNamedQueryImpl.class) ;	
	private List<EvaluateOperation> evaluateOps ;	

	public OptimisticNamedQueryImpl() {
		super() ;
	}
	
	public OptimisticNamedQueryImpl(QueryLanguage ql, String queryString, String baseURI) 
	throws RepositoryException {
		super(new PersistentNamedQueryImpl(ql, queryString, baseURI)) ;
		initialize() ;
	}
	
	public OptimisticNamedQueryImpl(PersistentNamedQuery namedQuery) 
	throws RepositoryException {
		super(namedQuery) ;
		initialize() ;
	}
	
	/** Persist new (persistent) named queries and place them in an optimistic wrapper */
	
	public static Map<URI, PersistentNamedQuery> persist(File dataDir, ValueFactory vf) 
	throws RepositoryException {
		Map<URI, PersistentNamedQuery> map = PersistentNamedQueryImpl.persist(dataDir,vf) ;
		Map<URI, PersistentNamedQuery> optimisticMap = new HashMap<URI,PersistentNamedQuery>() ;
		for (URI uri: map.keySet()) {
			optimisticMap.put(uri, new OptimisticNamedQueryImpl(map.get(uri))) ;
		}
		return optimisticMap ;
	}
	
	// based on OptimisticConnection.addChangeSet(), but using sailChanged event to trigger conflict detection
	
	public void sailChanged(SailChangedEvent event) {
		if (event instanceof SailChangeSetEvent) try {
			SailChangeSetEvent e = (SailChangeSetEvent) event ;
			
			// in exclusive mode no change sets are generated
			if (e.isExclusive()) {
				update(e.getTime()) ;
				return ;
			}
			else if (e.getSail() instanceof OptimisticSail) {
				if (findConflict(
					e.getStatementsAdded(), 
					e.getStatementsRemoved(), 
					e.getSail())) {
					update(e.getTime()) ;
				}
				return ;
			}			
		}
		catch (SailException e) {
			logger.error(e.getMessage()) ;			
		}
		// as regular listeners, named queries also receive standard change events without change-sets which should be ignored
		else if (event instanceof DefaultSailChangedEvent) {
			return ;
		}
		// update as a last resort
		update(System.currentTimeMillis()) ;
	}

	/** Store EvaluateOperations as a member list, call after null constructor + setter */
	
	public void initialize() throws RepositoryException {
		evaluateOps = new LinkedList<EvaluateOperation>() ;
		BasicNodeCollector collector = new BasicNodeCollector(getQuery());
		for (TupleExpr expr : collector.findBasicNodes()) {
			EvaluateOperation op = new EvaluateOperation(expr, false);
			evaluateOps.add(op) ;
		}
	}

	// based on OptimisticConnection.addChangeSet(), but using sailChanged event to trigger conflict detection

	private boolean findConflict(Model added, Model removed, Sail sail)
	throws SailException {
		for (EvaluateOperation op: evaluateOps) {

			if ((!added.isEmpty() && ((OptimisticSail) sail).effects(added, op)) ||
				(!removed.isEmpty() && ((OptimisticSail) sail).effects(removed, op)) )
				return true ;
		}
		return false;
	}
	
	
}

