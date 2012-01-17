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
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.NamedQuery;
import org.openrdf.repository.query.PersistentNamedQuery;
import org.openrdf.sail.SailChangedEvent;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.optimistic.helpers.NonExcludingFinder;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of named queries with optimistic update, wrapping a persistent
 * named query
 * 
 * @author Steve Battle
 * 
 */

class OptimisticNamedQuery implements NamedQuery, SailChangedListener {
	/**
	 * Persist new (persistent) named queries and place them in an optimistic
	 * wrapper
	 */

	public static Map<URI, OptimisticNamedQuery> persist(File dataDir)
			throws RepositoryException {
		Map<URI, PersistentNamedQuery> map = PersistentNamedQuery
				.persist(dataDir);
		Map<URI, OptimisticNamedQuery> optimisticMap = new HashMap<URI, OptimisticNamedQuery>();
		for (URI uri : map.keySet()) {
			optimisticMap.put(uri, new OptimisticNamedQuery(map.get(uri)));
		}
		return optimisticMap;
	}

	public static void desist(File dataDir, Map<URI, OptimisticNamedQuery> map)
			throws RepositoryException {
		Map<URI, PersistentNamedQuery> persistMap = new HashMap<URI, PersistentNamedQuery>();
		for (URI uri : map.keySet()) {
			persistMap.put(uri, map.get(uri).getDelegate());
		}
		PersistentNamedQuery.desist(dataDir, persistMap);
	}

	private PersistentNamedQuery delegate;

	/* Constructors */

	private final Logger logger = LoggerFactory
			.getLogger(OptimisticNamedQuery.class);
	private List<EvaluateOperation> evaluateOps;

	public OptimisticNamedQuery(URI uri, QueryLanguage ql, String queryString,
			String baseURI) throws RepositoryException {
		this(new PersistentNamedQuery(uri, ql, queryString, baseURI));
	}

	/**
	 * Store EvaluateOperations as a member list
	 */
	private OptimisticNamedQuery(PersistentNamedQuery namedQuery)
			throws RepositoryException {
		this.delegate = namedQuery;
		evaluateOps = new LinkedList<EvaluateOperation>();
		ParsedOperation parsed = getParsedOperation();
		if (parsed instanceof ParsedQuery) {
			TupleExpr qry = ((ParsedQuery) parsed).getTupleExpr();
			for (TupleExpr expr : new NonExcludingFinder().find(qry)) {
				EvaluateOperation op = new EvaluateOperation(expr, false);
				evaluateOps.add(op);
			}
		}
		if (parsed instanceof ParsedUpdate) {
			for (UpdateExpr up : ((ParsedUpdate) parsed).getUpdateExprs()) {
				for (TupleExpr expr : new NonExcludingFinder().find(up)) {
					EvaluateOperation op = new EvaluateOperation(expr, false);
					evaluateOps.add(op);
				}
			}
		}
	}

	public QueryLanguage getQueryLanguage() {
		return delegate.getQueryLanguage();
	}

	public String getQueryString() {
		return delegate.getQueryString();
	}

	public String getBaseURI() {
		return delegate.getBaseURI();
	}

	public long getResultLastModified() {
		return delegate.getResultLastModified();
	}

	public String getResultLastModifiedString() {
		return delegate.getResultLastModifiedString();
	}

	public String getResultTag() {
		return delegate.getResultTag();
	}

	public ParsedOperation getParsedOperation() {
		return delegate.getParsedOperation();
	}

	public void update(long time) {
		delegate.update(time);
	}

	// based on OptimisticConnection.addChangeSet(), but using sailChanged event
	// to trigger conflict detection

	public void sailChanged(SailChangedEvent event) {
		if (event instanceof SailChangeSetEvent) {
			SailChangeSetEvent e = (SailChangeSetEvent) event;

			// in exclusive mode no change sets are generated
			if (e.getAddedModel() == null || e.getRemovedModel() == null) {
				update(e.getTime());
			} else if (e.getSail() instanceof OptimisticSail) {
				if (conflicts(e.getAddedModel(), e.getRemovedModel(),
						(OptimisticSail) e.getSail())) {
					update(e.getTime());
				}
			}
		} else if (event.statementsAdded() || event.statementsRemoved()) {
			update(System.currentTimeMillis());
		}
	}

	private PersistentNamedQuery getDelegate() {
		return delegate;
	}

	// based on OptimisticConnection.addChangeSet(), but using sailChanged event
	// to trigger conflict detection

	private boolean conflicts(Model added, Model removed, OptimisticSail sail) {
		try {
			for (EvaluateOperation op : evaluateOps) {
				if (!added.isEmpty() && sail.effects(added, op))
					return true;
				if (!removed.isEmpty() && sail.effects(removed, op))
					return true;
			}
			return false;
		} catch (SailException e) {
			logger.error(e.toString(), e);
			return true;
		}
	}

}
