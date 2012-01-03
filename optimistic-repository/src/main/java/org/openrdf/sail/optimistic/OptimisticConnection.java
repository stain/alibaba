/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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

import static org.openrdf.query.algebra.StatementPattern.Scope.DEFAULT_CONTEXTS;
import static org.openrdf.query.algebra.StatementPattern.Scope.NAMED_CONTEXTS;
import info.aduna.concurrent.locks.Lock;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.UnionIteration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.helpers.SailUpdateExecutor;
import org.openrdf.sail.helpers.SailWrapper;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencySailException;
import org.openrdf.sail.optimistic.helpers.NonExcludingFinder;
import org.openrdf.sail.optimistic.helpers.ChangeWithReadSet;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optionally enforces snapshot and serializable isolation.
 * 
 * @author James Leigh
 *
 */
public class OptimisticConnection implements
		NotifyingSailConnection {
	interface AddOperation {
		void addNow(Resource subj, URI pred, Value obj, Resource... contexts)
				throws SailException;

		/** locked by this */
		int addLater(Resource subj, URI pred, Value obj, Resource... contexts);
	}

	interface RemoveOperation {
		void removeNow(Resource subj, URI pred, Value obj, Resource... contexts)
				throws SailException;

		/** locked by this */
		int removeLater(Statement st);
	}

	private static final int LARGE_BLOCK = 10000;
	private Logger logger = LoggerFactory.getLogger(OptimisticConnection.class);
	private OptimisticSail sail;
	private boolean snapshot;
	private boolean serializable;
	private volatile boolean active;
	/** If no other transactions */
	private volatile boolean exclusive;
	/** locked by this */
	private Model added = new LinkedHashModel();
	/** locked by this */
	private Model removed = new LinkedHashModel();
	/** locked by this */
	private Set<Resource> addedContexts = new HashSet<Resource>();
	/** locked by this */
	private Set<Resource> removedContexts = new HashSet<Resource>();
	/** locked by this */
	private Map<String, String> addedNamespaces = new HashMap<String, String>();
	/** locked by this */
	private Set<String> removedPrefixes = new HashSet<String>();
	/** locked by sail.getReadLock() then this */
	private Set<EvaluateOperation> read = new HashSet<EvaluateOperation>();
	private LinkedList<ChangeWithReadSet> changesets = new LinkedList<ChangeWithReadSet>(); 
	/** If sail.getWriteLock() */
	private volatile boolean prepared;
	private volatile ConcurrencyException conflict;
	private volatile SailChangeSetEvent event;
	private volatile boolean listenersIsEmpty = true;
	private Set<SailConnectionListener> listeners = new HashSet<SailConnectionListener>();
	private SailConnection delegate;

	public OptimisticConnection(OptimisticSail sail, SailConnection delegate) {
		this.sail = sail;
		this.delegate = delegate;
	}

	public SailConnection getWrappedConnection() {
		return delegate;
	}

	public String toString() {
		return getWrappedConnection().toString();
	}

	public boolean isSnapshot() {
		return snapshot;
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	public boolean isSerializable() {
		return serializable;
	}

	public void setSerializable(boolean serializable) {
		this.serializable = serializable;
	}

	public void close() throws SailException {
		if (active) {
			rollback();
		}
		delegate.close();
	}

	/** locked by this */
	public Model getAddedModel() {
		return added;
	}

	/** locked by this */
	public Model getRemovedModel() {
		return removed;
	}

	/** locked by this */
	public Set<EvaluateOperation> getReadOperations() {
		return read;
	}

	public void addConnectionListener(SailConnectionListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
			listenersIsEmpty = false;
		}
	}

	public void removeConnectionListener(SailConnectionListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public boolean isOpen() throws SailException {
		return delegate.isOpen();
	}

	public boolean isAutoCommit() throws SailException {
		return !active;
	}

	public void begin() throws SailException {
		synchronized (this) {
			assert active == false;
			active = true;
			conflict = null;
			event = new SailChangeSetEvent(sail);
			changesets.clear();
			read.clear();
			added.clear();
			removed.clear();
		}
		try {
			sail.begin(this);
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
	}

	public void commit() throws SailException {
		if (isAutoCommit())
			return;
		synchronized (this) {
			try {
				if (!prepared) {
					prepare();
				}
				if (exclusive) {
					logger.debug("Releasing exclusive store lock");
				} else {
					if (sail.isListenerPresent()) {
						event.setAddedModel(new LinkedHashModel(added));
						event.setRemovedModel(new LinkedHashModel(removed));
					}
					flush();
				}
				delegate.commit();
				active = false;
				conflict = null;
				prepared = false;
				exclusive = false;
				read.clear();
				addedContexts.clear();
				removedContexts.clear();
				changesets.clear();
				event.setTime(System.currentTimeMillis());
				sail.endAndNotify(this, event);
				event = null;
			} finally {
				// close resources opened in this transaction scope
				sail.end(this);
			}
		}
	}

	public void rollback() throws SailException {
		try {
			if (exclusive) {
				logger.debug("Releasing exclusive store lock");
			}
			delegate.rollback();
			synchronized (this) {
				added.clear();
				removed.clear();
				read.clear();
				addedContexts.clear();
				removedContexts.clear();
				changesets.clear();
				active = false;
				conflict = null;
				prepared = false;
				exclusive = false;
			}
			event = null;
		}
		finally {
			// close resources opened in this transaction scope
			sail.end(this) ;
		}
	}

	public void executeUpdate(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		checkForWriteConflict();
		// SailUpdateExecutor may call evaluate on a new connection
		// override evaluate to record the observed state in this transaction
		SailWrapper wrap = new SailWrapper(sail.getBaseSail()) {
			public SailConnection getConnection() throws SailException {
				return new SailConnectionWrapper(OptimisticConnection.this) {
					public void close() throws SailException {
						// ignore
					}
				};
			}
		};
		SailUpdateExecutor executor = new SailUpdateExecutor(wrap, this);
		executor.executeUpdate(updateExpr, dataset, bindings, includeInferred);
	}

	public void clear(Resource... contexts) throws SailException {
		if (exclusive) {
			delegate.clear(contexts);
		} else {
			removeStatements(null, null, null, contexts);
			if (contexts != null && contexts.length > 0) {
				synchronized (this) {
					for (Resource ctx : contexts) {
						if (ctx != null) {
							removedContexts.add(ctx);
							addedContexts.remove(ctx);
						}
					}
				}
			}
		}
	}

	public CloseableIteration<? extends Resource, SailException> getContextIDs()
			throws SailException {
		final CloseableIteration<? extends Resource, SailException> contextIDs = delegate.getContextIDs();
		if (!active || exclusive) {
			return contextIDs;
		} else {
			Iterator<Resource> added = null;
			Set<Resource> removed = null;
			synchronized (this) {
				if (!addedContexts.isEmpty()) {
					added = new ArrayList<Resource>(addedContexts).iterator();
				}
				if (!removedContexts.isEmpty()) {
					removed = new HashSet(removedContexts);
				}
			}
			if (added == null && removed == null)
				return contextIDs;
			final Iterator<Resource> addedIter = added;
			final Set<Resource> removedSet = removed;
			return new CloseableIteration<Resource, SailException>() {
				Resource next;
	
				public void close() throws SailException {
					contextIDs.close();
				}
	
				public boolean hasNext() throws SailException {
					if (addedIter != null && addedIter.hasNext())
						return true;
					while (next == null && contextIDs.hasNext()) {
						next = contextIDs.next();
						if (removedSet != null && removedSet.contains(next)) {
							next = null;
						}
					}
					return next != null;
				}
	
				public Resource next() throws SailException {
					if (addedIter != null && addedIter.hasNext())
						return addedIter.next();
					try {
						if (hasNext())
							return next;
						throw new NoSuchElementException();
					} finally {
						next = null;
					}
				}
	
				public void remove() throws SailException {
					throw new UnsupportedOperationException();
				}
				
			};
		}
	}

	public void clearNamespaces() throws SailException {
		if (exclusive) {
			delegate.clearNamespaces();
		} else {
			LinkedList<String> list = new LinkedList<String>();
			CloseableIteration<? extends Namespace, SailException> ns;
			ns = delegate.getNamespaces();
			while (ns.hasNext()) {
				list.add(ns.next().getPrefix());
			}
			synchronized (this) {
				removedPrefixes.clear();
				removedPrefixes.addAll(list);
				addedNamespaces.clear();
			}
		}
	}

	public void removeNamespace(String prefix) throws SailException {
		if (exclusive) {
			delegate.removeNamespace(prefix);
		} else {
			synchronized (this) {
				removedPrefixes.add(prefix);
				addedNamespaces.remove(prefix);
			}
		}
	}

	public void setNamespace(String prefix, String name) throws SailException {
		if (exclusive) {
			delegate.setNamespace(prefix, name);
		} else {
			synchronized (this) {
				removedPrefixes.add(prefix);
				addedNamespaces.put(prefix, name);
			}
		}
	}

	public String getNamespace(String prefix) throws SailException {
		if (!active || exclusive) {
			return delegate.getNamespace(prefix);
		} else {
			synchronized (this) {
				if (addedNamespaces.containsKey(prefix))
					return addedNamespaces.get(prefix);
				if (removedPrefixes.contains(prefix))
					return null;
			}
			return delegate.getNamespace(prefix);
		}
	}

	public CloseableIteration<? extends Namespace, SailException> getNamespaces()
			throws SailException {
		final CloseableIteration<? extends Namespace, SailException> namespaces;
		namespaces = delegate.getNamespaces();
		Iterator<Map.Entry<String, String>> added = null;
		Set<String> removed = null;
		synchronized (this) {
			if (!addedNamespaces.isEmpty()) {
				added = new HashMap<String, String>(addedNamespaces).entrySet().iterator();
			}
			if (!removedPrefixes.isEmpty()) {
				removed = new HashSet(removedPrefixes);
			}
		}
		if (added == null && removed == null)
			return namespaces;
		final Iterator<Map.Entry<String, String>> addedIter = added;
		final Set<String> removedSet = removed;
		return new CloseableIteration<Namespace, SailException>() {
			Namespace next;
	
			public void close() throws SailException {
				namespaces.close();
			}
	
			public boolean hasNext() throws SailException {
				if (addedIter != null && addedIter.hasNext())
					return true;
				while (next == null && namespaces.hasNext()) {
					next = namespaces.next();
					if (removedSet != null && removedSet.contains(next.getPrefix())) {
						next = null;
					}
				}
				return next != null;
			}
	
			public Namespace next() throws SailException {
				if (addedIter != null && addedIter.hasNext()) {
					Entry<String, String> e = addedIter.next();
					return new NamespaceImpl(e.getKey(), e.getValue());
				}
				try {
					if (hasNext())
						return next;
					throw new NoSuchElementException();
				} finally {
					next = null;
				}
			}
	
			public void remove() throws SailException {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	public void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		checkForWriteConflict();
		AddOperation op = new AddOperation() {

			public int addLater(Resource subj, URI pred, Value obj,
					Resource... contexts) {
				Resource[] ctxs = notNull(contexts);
				if (ctxs.length == 0) {
					ctxs = new Resource[] { null };
				}
				removed.remove(subj, pred, obj, ctxs);
				added.add(subj, pred, obj, contexts);
				return added.size();
			}

			public void addNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.addStatement(subj, pred, obj,
						contexts);
			}
		};
		add(op, subj, pred, obj, contexts);
	}

	public void removeStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		checkForWriteConflict();
		RemoveOperation op = new RemoveOperation() {

			public int removeLater(Statement st) {
				added.remove(st);
				removed.add(st);
				return removed.size();
			}

			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.removeStatements(subj, pred, obj,
						contexts);
			}
		};
		remove(op, subj, pred, obj, false, contexts);
	}

	public long size(Resource... contexts) throws SailException {
		checkForReadConflict();
		long size = delegate.size(contexts);
		if (!active || exclusive)
			return size;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				read(null, null, null, true, contexts);
				int rsize = removed.filter(null, null, null, contexts).size();
				int asize = added.filter(null, null, null, contexts).size();
				return size - rsize + asize;
			}
		} finally {
			lock.release();
		}
	}

	public CloseableIteration<? extends Statement, SailException> getStatements(
			Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) throws SailException {
		checkForReadConflict();
		CloseableIteration<? extends Statement, SailException> result;
		result = delegate.getStatements(subj, pred, obj, inf, contexts);
		if (!active || exclusive)
			return result;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				read(subj, pred, obj, inf, contexts);
				Model excluded = removed.filter(subj, pred, obj, contexts);
				Model included = added.filter(subj, pred, obj, contexts);
				if (included.isEmpty() && excluded.isEmpty())
					return result;
				if (!excluded.isEmpty()) {
					final Set<Statement> set = new HashSet<Statement>(excluded);
					result = new FilterIteration<Statement, SailException>(
							result) {
						@Override
						protected boolean accept(Statement stmt)
								throws SailException {
							return !set.contains(stmt);
						}
					};
				}
				HashSet<Statement> set = new HashSet<Statement>(included);
				CloseableIteration<Statement, SailException> incl;
				incl = new CloseableIteratorIteration<Statement, SailException>(
						set.iterator());
				return new UnionIteration<Statement, SailException>(incl,result);
			}
		} finally {
			lock.release();
		}
	}

	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(
			TupleExpr query, Dataset dataset, BindingSet bindings, boolean inf)
			throws SailException {
		return evaluateWith(delegate, query, dataset, bindings, inf);
	}

	CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateWith(
			SailConnection delegate, TupleExpr query, Dataset dataset,
			BindingSet bindings, boolean inf) throws SailException {
		CloseableIteration<? extends BindingSet, QueryEvaluationException> result;
		checkForReadConflict();
		if (!active || exclusive)
			return delegate.evaluate(query, dataset, bindings, inf);

		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				if (!added.isEmpty() || !removed.isEmpty()) {
					query = new QueryRoot(query.clone());
					DeltaMerger merger = new DeltaMerger(added, removed);
					merger.optimize(query, dataset, bindings);
				}
			}
		} finally {
			lock.release();
		}
		result = delegate.evaluate(query, dataset, bindings, inf);
		lock = sail.getReadLock();
		try {
			synchronized (this) {
				for (TupleExpr expr : new NonExcludingFinder().find(query)) {
					addRead(new EvaluateOperation(dataset, expr, bindings, inf));
				}
			}
		} finally {
			lock.release();
		}
		return result;
	}

	void checkForReadConflict() throws SailException {
		if (prepared)
			throw new IllegalStateException();
	}

	void checkForWriteConflict() throws SailException {
		if (prepared)
			throw new IllegalStateException();
	}

	boolean isConflict() {
		return conflict != null;
	}

	void setConflict(ConcurrencyException exc) {
		this.conflict = exc;
	}

	void addChangeSet(Model added, Model removed) {
		changesets.add(new ChangeWithReadSet(added, removed));
	}

	/** locked by this */
	synchronized void flush() throws SailException {
		for (String prefix : removedPrefixes) {
			delegate.removeNamespace(prefix);
		}
		removedPrefixes.clear();
		for (Map.Entry<String, String> e : addedNamespaces.entrySet()) {
			delegate.setNamespace(e.getKey(), e.getValue());
		}
		addedNamespaces.clear();
		if (!removedContexts.isEmpty()) {
			delegate.clear(removedContexts.toArray(new Resource[removedContexts.size()]));
			removedContexts.clear();
		}
		if (!removed.isEmpty()) {
			logger.debug("Removing {} statements", removed.size());
			for (Statement st : removed) {
				delegate.removeStatements(st.getSubject(), st.getPredicate(),
						st.getObject(), st.getContext());
			}
			removed.clear();
		}
		addedContexts.clear();
		if (!added.isEmpty()) {
			logger.debug("Adding {} statements", removed.size());
			for (Statement st : added) {
				delegate.addStatement(st.getSubject(), st.getPredicate(), st
						.getObject(), st.getContext());
			}
			added.clear();
		}
	}

	void add(AddOperation op, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		if (exclusive) {
			op.addNow(subj, pred, obj, contexts);
		} else {
			int size;
			synchronized (this) {
				size = op.addLater(subj, pred, obj, contexts);
			}
			if (listenersIsEmpty && size > 0 && size % LARGE_BLOCK == 0) {
				if (sail.exclusive(this)) {
					String msg = "Switching to exclusive store mode after adding {} triples";
					logger.debug(msg, size);
					synchronized (this) {
						exclusive = true;
						read.clear();
						changesets.clear();
						flush();
					}
				}
			}
		}
		event.setStatementsAdded(true);
		Resource[] ctxs = notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = new Resource[] { null };
		} else {
			synchronized (this) {
				for (Resource ctx : ctxs) {
					if (ctx != null) {
						removedContexts.remove(ctx);
						addedContexts.add(ctx);
					}
				}
			}
		}
		for (SailConnectionListener listener : getListeners()) {
			for (Resource ctx : ctxs) {
				Statement st = new ContextStatementImpl(subj, pred, obj, ctx);
				listener.statementAdded(st);
			}
		}
	}

	/**
	 * @return If RemoveOperation was called
	 */
	boolean remove(RemoveOperation op, Resource subj, URI pred, Value obj,
			boolean inf, Resource... contexts) throws SailException {
		boolean called = false;
		event.setStatementsRemoved(true);
		if (exclusive) {
			called = true;
			op.removeNow(subj, pred, obj, contexts);
		} else {
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = getStatements(subj, pred, obj, inf, contexts);
			try {
				while (stmts.hasNext()) {
					called = true;
					int size;
					synchronized (this) {
						size = op.removeLater(stmts.next());
					}
					if (listenersIsEmpty && size > 0 && size % LARGE_BLOCK == 0
							&& sail.exclusive(this)) {
						String msg = "Switching to exclusive store mode after removing {} triples";
						logger.debug(msg, size);
						synchronized (this) {
							exclusive = true;
							read.clear();
							changesets.clear();
						}
						break;
					}
				}
			} finally {
				stmts.close();
			}
			if (exclusive) {
				flush();
				delegate.removeStatements(subj, pred, obj, contexts);
			}
		}
		if (!listenersIsEmpty && called) {
			Set<SailConnectionListener> listeners = getListeners();
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = delegate.getStatements(subj, pred, obj, inf, contexts);
			try {
				while (stmts.hasNext()) {
					for (SailConnectionListener listener : listeners) {
						listener.statementRemoved(stmts.next());
					}
				}
			} finally {
				stmts.close();
			}
		}
		return called;
	}

	private Set<SailConnectionListener> getListeners() {
		synchronized (listeners) {
			if (listeners.isEmpty())
				return Collections.emptySet();
			return new HashSet<SailConnectionListener>(listeners);
		}
	}

	private void prepare() throws SailException {
		try {
			sail.prepare(this);
			prepared = true;
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		if (isSerializable() && conflict != null && !isReadOnly()) {
			try {
				throw new ConcurrencySailException(conflict);
			} finally {
				rollback();
			}
		} else if (!changesets.isEmpty()) {
			synchronized (this) {
				conflict = findConflict(changesets, conflict);
			}
			if (conflict != null) {
				try {
					throw new ConcurrencySailException(conflict);
				} finally {
					rollback();
				}
			}
		}
	}

	private ConcurrencyException findConflict(LinkedList<ChangeWithReadSet> changesets,
			ConcurrencyException cause) throws SailException {
		for (ChangeWithReadSet cs : changesets) {
			Model added = cs.getAdded();
			Model removed = cs.getRemoved();
			for (EvaluateOperation op : cs.getReadOperations()) {
				if (!added.isEmpty() && sail.effects(added, op)) {
					return phantom(added, op, cause);
				}
				if (!removed.isEmpty() && sail.effects(removed, op)) {
					return phantom(removed, op, cause);
				}
			}
		}
		return null;
	}

	private ConcurrencyException phantom(Model delta, EvaluateOperation op,
			ConcurrencyException cause) {
		return new ConcurrencyException("Observed Inconsistent State", op,
				delta, cause);
	}

	private boolean isReadOnly() {
		return added.isEmpty() && removed.isEmpty();
	}

	/** locked by this */
	private void read(Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) {
		contexts = notNull(contexts);
		Var subjVar = new Var("subj", subj);
		Var predVar = new Var("pred", pred);
		Var objVar = new Var("obj", obj);
		TupleExpr union = null;
		if (contexts.length == 0) {
			union = new StatementPattern(subjVar, predVar, objVar);
		}
		for (Resource ctx : contexts) {
			Var ctxVar = new Var("ctx", ctx);
			Scope scope = ctx == null ? DEFAULT_CONTEXTS : NAMED_CONTEXTS;
			TupleExpr sp = new StatementPattern(scope, subjVar, predVar,
					objVar, ctxVar);
			union = union == null ? sp : new Union(union, sp);
		}
		addRead(new EvaluateOperation(union, inf));
	}

	private void addRead(EvaluateOperation op) {
		if (isSnapshot()) {
			read.add(op);
			for (ChangeWithReadSet changeset : changesets) {
				changeset.addRead(op);
			}
		}
	}

	private Resource[] notNull(Resource[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
	}
}
