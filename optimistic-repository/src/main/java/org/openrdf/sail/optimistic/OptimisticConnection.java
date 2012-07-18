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
import java.util.LinkedHashMap;
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
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.MemoryOverflowModel;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.UnionModel;
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
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.ConstantOptimizer;
import org.openrdf.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.evaluation.impl.IterativeEvaluationOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryModelNormalizer;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.helpers.SailUpdateExecutor;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencySailException;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.openrdf.sail.optimistic.helpers.InconsistentChange;
import org.openrdf.sail.optimistic.helpers.InvalidTripleSource;
import org.openrdf.sail.optimistic.helpers.NonExcludingFinder;

/**
 * Optionally enforces snapshot and serializable isolation.
 * 
 * @author James Leigh
 *
 */
public class OptimisticConnection extends TransactionalSailConnectionWrapper implements
		NotifyingSailConnection {
	abstract static class Operation {
		final UpdateExpr updateExpr;
		final Dataset dataset;
		final BindingSet bindings;

		public Operation() {
			this.updateExpr = null;
			this.dataset = null;
			this.bindings = null;
		}

		public Operation(UpdateExpr updateExpr, Dataset dataset,
				BindingSet bindings) {
			this.updateExpr = updateExpr;
			this.dataset = dataset;
			this.bindings = bindings;
		}

		public boolean isUpdate() {
			return updateExpr != null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((bindings == null) ? 0 : bindings.hashCode());
			result = prime * result
					+ ((dataset == null) ? 0 : dataset.hashCode());
			result = prime * result
					+ ((updateExpr == null) ? 0 : updateExpr.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Operation other = (Operation) obj;
			if (bindings == null) {
				if (other.bindings != null)
					return false;
			} else if (!bindings.equals(other.bindings))
				return false;
			if (dataset == null) {
				if (other.dataset != null)
					return false;
			} else if (!dataset.equals(other.dataset))
				return false;
			if (updateExpr == null) {
				if (other.updateExpr != null)
					return false;
			} else if (!updateExpr.equals(other.updateExpr))
				return false;
			return true;
		}
	}
	abstract static class AddOperation extends Operation {

		public AddOperation() {
			super();
		}

		public AddOperation(UpdateExpr updateExpr, Dataset dataset,
				BindingSet bindings) {
			super(updateExpr, dataset, bindings);
		}

		public abstract void addNow(Resource subj, URI pred, Value obj,
				Resource... contexts) throws SailException;

		/** locked by this */
		public abstract void addLater(Resource subj, URI pred, Value obj,
				Resource... contexts);
	}

	abstract static class RemoveOperation extends Operation{

		public RemoveOperation() {
			super();
		}

		public RemoveOperation(UpdateExpr updateExpr, Dataset dataset,
				BindingSet bindings) {
			super(updateExpr, dataset, bindings);
		}

		public abstract void removeNow(Resource subj, URI pred, Value obj,
				Resource... contexts) throws SailException;

		/** locked by this */
		public abstract void removeLater(Statement st);
	}

	private OptimisticSail sail;
	private boolean readSnapshot = true;
	private boolean snapshot;
	private boolean serializable;
	private volatile boolean active;
	/** locked by this */
	private final Map<AddOperation,MemoryOverflowModel> added = new LinkedHashMap<AddOperation,MemoryOverflowModel>();
	/** locked by this */
	private final Map<RemoveOperation,MemoryOverflowModel> removed = new LinkedHashMap<RemoveOperation,MemoryOverflowModel>();
	/** locked by this */
	private Set<Resource> addedContexts = new HashSet<Resource>();
	/** locked by this */
	private Set<Resource> removedContexts = new HashSet<Resource>();
	/** locked by this */
	private Map<String, String> addedNamespaces = new HashMap<String, String>();
	/** locked by this */
	private Set<String> removedPrefixes = new HashSet<String>();
	/** locked by observations */
	private final Set<EvaluateOperation> observations = new HashSet<EvaluateOperation>();
	/** locked by observations */
	private final LinkedList<InconsistentChange> changes = new LinkedList<InconsistentChange>();
	/** If sail.getWriteLock() */
	private volatile boolean prepared;
	private volatile SailChangeSetEvent event;
	private volatile boolean listenersIsEmpty = true;
	private Set<SailConnectionListener> listeners = new HashSet<SailConnectionListener>();
	private SailConnection delegate;
	private final AddOperation explicitAdd = new AddOperation() {

		public void addLater(Resource subj, URI pred, Value obj,
				Resource... contexts) {
			Resource[] ctxs = notNull(contexts);
			if (ctxs.length == 0) {
				ctxs = new Resource[] { null };
			}
			getRemovedModel().remove(subj, pred, obj, ctxs);
			added.get(this).add(subj, pred, obj, contexts);
		}

		public void addNow(Resource subj, URI pred, Value obj,
				Resource... contexts) throws SailException {
			delegate.addStatement(subj, pred, obj,
					contexts);
		}
	};
	private final RemoveOperation explicitRemove = new RemoveOperation() {

		public void removeLater(Statement st) {
			getAddedModel().remove(st);
			removed.get(this).add(st);
		}

		public void removeNow(Resource subj, URI pred, Value obj,
				Resource... contexts) throws SailException {
			delegate.removeStatements(subj, pred, obj,
					contexts);
		}
	};

	public OptimisticConnection(OptimisticSail sail, SailConnection delegate) {
		super(delegate);
		this.sail = sail;
		this.delegate = delegate;
		resetChangeModel();
	}

	public SailConnection getWrappedConnection() {
		return delegate;
	}

	public String toString() {
		return getWrappedConnection().toString();
	}

	public boolean isReadSnapshot() {
		return readSnapshot || isSnapshot();
	}

	public void setReadSnapshot(boolean readSnapshot) throws SailException, InterruptedException {
		if (!readSnapshot && this.readSnapshot) {
			if (active) {
				sail.exclusive(this);
			}
			synchronized (this) {
				releaseObservedChange();
				flush();
			}
			setSnapshot(false);
		}
		this.readSnapshot = readSnapshot;
	}

	public boolean isSnapshot() {
		return snapshot || isSerializable();
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
		if (!snapshot) {
			setSerializable(false);
		}
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
	public synchronized Model getAddedModel() {
		return new UnionModel(added.values().toArray(new Model[added.size()]));
	}

	/** locked by this */
	public synchronized Model getRemovedModel() {
		return new UnionModel(removed.values().toArray(new Model[removed.size()]));
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

	public boolean isActive() {
		return active;
	}

	public void begin() throws SailException {
		if (isActive())
			throw new IllegalStateException("Transaction already started");
		super.begin();
		synchronized (this) {
			assert active == false;
			active = true;
			event = new SailChangeSetEvent(sail);
			releaseObservedChange();
			resetChangeModel();
		}
		try {
			sail.begin(this);
			if (!isReadSnapshot()) {
				sail.exclusive(this);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public synchronized void prepare() throws SailException {
		super.prepare();
		try {
			sail.prepare(this);
			prepared = true;
		} catch (InterruptedException e) {
			rollback();
			throw new SailException(e);
		}
		ConcurrencyException conflict = getConcurrencyConflict();
		if (conflict != null) {
			try {
				throw new ConcurrencySailException(conflict);
			} finally {
				rollback();
			}
		}
	}

	public void commit() throws SailException {
		if (!isActive())
			return;
		synchronized (this) {
			try {
				if (!isActive())
					return;
				if (!prepared) {
					prepare();
				}
				if (isReadSnapshot() && sail.isListenerPresent()) {
					event.setAddedModel(getAddedModel());
					event.setRemovedModel(getRemovedModel());
				}
				flush();
				super.commit();
			} catch (SailException e) {
				rollback();
				throw e;
			}
		}
	}

	public void executeUpdate(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		checkForWriteConflict();
		if (isReadSnapshot()) {
			ValueFactory vf = sail.getValueFactory();
			final AddOperation add = createInsertOperation(updateExpr, dataset,
					bindings);
			final RemoveOperation remove = createDeleteOperation(updateExpr,
					dataset, bindings);
			SailUpdateExecutor executor = new SailUpdateExecutor(
					new SailConnectionWrapper(this) {

						@Override
						public void addStatement(Resource subj, URI pred,
								Value obj, Resource... contexts)
								throws SailException {
							add(add, subj, pred, obj, contexts);
						}

						@Override
						public void removeStatements(Resource subj, URI pred,
								Value obj, Resource... contexts)
								throws SailException {
							remove(remove, subj, pred, obj, false, contexts);
						}

					}, vf, true);
			executor.executeUpdate(updateExpr, dataset, bindings,
					includeInferred);
		} else {
			super.executeUpdate(updateExpr, dataset, bindings, includeInferred);
		}
	}

	public void clear(Resource... contexts) throws SailException {
		if (isReadSnapshot()) {
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
		} else {
			delegate.clear(contexts);
		}
	}

	public CloseableIteration<? extends Resource, SailException> getContextIDs()
			throws SailException {
		final CloseableIteration<? extends Resource, SailException> contextIDs = delegate.getContextIDs();
		if (!active || !isReadSnapshot())
			return contextIDs;
		Iterator<Resource> added = null;
		Set<Resource> removed = null;
		synchronized (this) {
			if (!addedContexts.isEmpty()) {
				added = new ArrayList<Resource>(addedContexts).iterator();
			}
			if (!removedContexts.isEmpty()) {
				removed = new HashSet<Resource>(removedContexts);
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

	public void clearNamespaces() throws SailException {
		if (isReadSnapshot()) {
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
		} else {
			delegate.clearNamespaces();
		}
	}

	public void removeNamespace(String prefix) throws SailException {
		if (isReadSnapshot()) {
			synchronized (this) {
				removedPrefixes.add(prefix);
				addedNamespaces.remove(prefix);
			}
		} else {
			delegate.removeNamespace(prefix);
		}
	}

	public void setNamespace(String prefix, String name) throws SailException {
		if (isReadSnapshot()) {
			synchronized (this) {
				removedPrefixes.add(prefix);
				addedNamespaces.put(prefix, name);
			}
		} else {
			delegate.setNamespace(prefix, name);
		}
	}

	public String getNamespace(String prefix) throws SailException {
		if (!active || !isReadSnapshot())
			return delegate.getNamespace(prefix);
		synchronized (this) {
			if (addedNamespaces.containsKey(prefix))
				return addedNamespaces.get(prefix);
			if (removedPrefixes.contains(prefix))
				return null;
		}
		return delegate.getNamespace(prefix);
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
				removed = new HashSet<String>(removedPrefixes);
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
		add(explicitAdd, subj, pred, obj, contexts);
	}

	public void removeStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		checkForWriteConflict();
		remove(explicitRemove, subj, pred, obj, false, contexts);
	}

	@Override
	public void executeInsert(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		AddOperation op = createInsertOperation(updateExpr, dataset, bindings);
		add(op, subj, pred, obj, contexts);
	}

	@Override
	public void executeDelete(UpdateExpr updateExpr, Dataset dataset,
			BindingSet bindings, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		RemoveOperation op = createDeleteOperation(updateExpr, dataset,
				bindings);
		remove(op, subj, pred, obj, false, contexts);
	}

	public long size(Resource... contexts) throws SailException {
		checkForReadConflict();
		long size = delegate.size(contexts);
		if (!active || !isReadSnapshot())
			return size;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				read(null, null, null, true, contexts);
				Model removed = getRemovedModel().filter(null, null, null, contexts);
				Model added = getAddedModel().filter(null, null, null, contexts);
				return size - removed.size() + added.size();
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
		if (!active || !isReadSnapshot())
			return result;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				read(subj, pred, obj, inf, contexts);
				Model excluded = getRemovedModel().filter(subj, pred, obj, contexts);
				Model included = getAddedModel().filter(subj, pred, obj, contexts);
				if (included.isEmpty() && excluded.isEmpty())
					return result;
				if (!excluded.isEmpty()) {
					final MemoryOverflowModel set;
					set = new MemoryOverflowModel(excluded);
					result = new FilterIteration<Statement, SailException>(
							result) {
						protected boolean accept(Statement stmt)
								throws SailException {
							return !set.contains(stmt);
						}
					};
				}
				final MemoryOverflowModel set;
				set = new MemoryOverflowModel(included);
				final Iterator<Statement> iter = set.iterator();
				CloseableIteration<Statement, SailException> incl;
				incl = new CloseableIteratorIteration<Statement, SailException>(
						iter);
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

	protected void end(boolean success) {
		try {
			synchronized (this) {
				active = false;
				prepared = false;
				addedContexts.clear();
				removedContexts.clear();
				releaseObservedChange();
				if (success) {
					event.setTime(System.currentTimeMillis());
					sail.endAndNotify(this, event);
				}
				event = null;
				resetChangeModel();
			}
		}
		finally {
			// close resources opened in this transaction scope
			sail.end(this) ;
		}
	}

	CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateWith(
			SailConnection delegate, TupleExpr query, Dataset dataset,
			BindingSet bindings, boolean inf) throws SailException {
		CloseableIteration<? extends BindingSet, QueryEvaluationException> result;
		checkForReadConflict();
		if (!active || !isReadSnapshot())
			return delegate.evaluate(query, dataset, bindings, inf);

		final DeltaMerger merger;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				Model added = getAddedModel();
				Model removed = getRemovedModel();
				if (added.isEmpty() && removed.isEmpty()) {
					merger = null;
				} else {
					query = optimize(query, dataset, bindings);
					merger = new DeltaMerger(added, removed);
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
		addedContexts.clear();
		for (Map.Entry<RemoveOperation, MemoryOverflowModel> e : removed
				.entrySet()) {
			RemoveOperation op = e.getKey();
			Model model = e.getValue();
			if (op.isUpdate()) {
				for (Statement st : model) {
					super.executeDelete(op.updateExpr, op.dataset, op.bindings,
							st.getSubject(), st.getPredicate(), st.getObject(),
							st.getContext());
				}
			} else {
				for (Statement st : model) {
					super.removeStatements(st.getSubject(), st.getPredicate(),
							st.getObject(), st.getContext());
				}
			}
		}
		for (Map.Entry<AddOperation, MemoryOverflowModel> e : added.entrySet()) {
			AddOperation op = e.getKey();
			Model model = e.getValue();
			if (op.isUpdate()) {
				for (Statement st : model) {
					super.executeInsert(op.updateExpr, op.dataset, op.bindings,
							st.getSubject(), st.getPredicate(), st.getObject(),
							st.getContext());
				}
			} else {
				for (Statement st : model) {
					super.addStatement(st.getSubject(), st.getPredicate(),
							st.getObject(), st.getContext());
				}
			}
		}
	}

	void add(AddOperation op, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		if (isReadSnapshot()) {
			synchronized (this) {
				op.addLater(subj, pred, obj, contexts);
			}
		} else {
			op.addNow(subj, pred, obj, contexts);
		}
		setStatementsAdded();
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
		setStatementsRemoved();
		if (isReadSnapshot()) {
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = getStatements(subj, pred, obj, inf, contexts);
			try {
				while (stmts.hasNext()) {
					called = true;
					synchronized (this) {
						op.removeLater(stmts.next());
					}
				}
			} finally {
				stmts.close();
			}
		} else {
			called = true;
			op.removeNow(subj, pred, obj, contexts);
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

	/**
	 * Checks this change to see if it is inconsistent with the observed state
	 * of the store.
	 */
	void changed(Model added, Model removed)
			throws SailException {
		synchronized (observations) {
			for (EvaluateOperation op : observations) {
				if (!added.isEmpty() && sail.effects(added, op)) {
					ConcurrencyException inc = inconsistency(op);
					changes.add(new InconsistentChange(added, removed, inc));
					break;
				} else if (!removed.isEmpty() && sail.effects(removed, op)) {
					ConcurrencyException inc = inconsistency(op);
					changes.add(new InconsistentChange(added, removed, inc));
					break;
				}
			}
		}
	}

	private AddOperation createInsertOperation(UpdateExpr updateExpr,
			Dataset dataset, BindingSet bindings) {
		AddOperation op = new AddOperation(updateExpr, dataset, bindings) {
	
			public void addLater(Resource subj, URI pred, Value obj,
					Resource... contexts) {
				Resource[] ctxs = notNull(contexts);
				if (ctxs.length == 0) {
					ctxs = new Resource[] { null };
				}
				getRemovedModel().remove(subj, pred, obj, ctxs);
				added.get(this).add(subj, pred, obj, contexts);
			}
	
			public void addNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.addStatement(subj, pred, obj,
						contexts);
			}
		};
		if (added.containsKey(op)) {
			for (AddOperation key : added.keySet()) {
				if (key.equals(op))
					return key;
			}
		} else {
			added.put(op, new MemoryOverflowModel());
		}
		return op;
	}

	private RemoveOperation createDeleteOperation(UpdateExpr updateExpr,
			Dataset dataset, BindingSet bindings) {
		RemoveOperation op = new RemoveOperation(updateExpr, dataset, bindings) {
	
			public void removeLater(Statement st) {
				getAddedModel().remove(st);
				removed.get(this).add(st);
			}
	
			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				delegate.removeStatements(subj, pred, obj,
						contexts);
			}
		};
		if (removed.containsKey(op)) {
			for (RemoveOperation key : removed.keySet()) {
				if (key.equals(op))
					return key;
			}
		} else {
			removed.put(op, new MemoryOverflowModel());
		}
		return op;
	}

	private ConcurrencyException inconsistency(EvaluateOperation op) {
		return new ConcurrencyException("Observed State has Changed", op);
	}

	private synchronized void setStatementsAdded() {
		if (event != null) {
			event.setStatementsAdded(true);
		}
	}

	private synchronized void setStatementsRemoved() {
		if (event != null) {
			event.setStatementsRemoved(true);
		}
	}

	private TupleExpr optimize(TupleExpr query, Dataset dataset,
			BindingSet bindings) {
		ValueFactory vf = sail.getValueFactory();
		InvalidTripleSource source = new InvalidTripleSource(vf);
		EvaluationStrategyImpl strategy = new EvaluationStrategyImpl(source);
		if (query instanceof QueryRoot) {
			query = query.clone();
		} else {
			query = new QueryRoot(query.clone());
		}
		new BindingAssigner().optimize(query, dataset, bindings);
		new ConstantOptimizer(strategy).optimize(query, dataset, bindings);
		new CompareOptimizer().optimize(query, dataset, bindings);
		new ConjunctiveConstraintSplitter().optimize(query, dataset, bindings);
		new DisjunctiveConstraintOptimizer().optimize(query, dataset, bindings);
		new SameTermFilterOptimizer().optimize(query, dataset, bindings);
		new QueryModelNormalizer().optimize(query, dataset, bindings);
		new QueryJoinOptimizer().optimize(query, dataset, bindings);
		new IterativeEvaluationOptimizer().optimize(query, dataset, bindings);
		return query;
	}

	private void resetChangeModel() {
		added.clear();
		removed.clear();
		added.put(explicitAdd, new MemoryOverflowModel());
		removed.put(explicitRemove, new MemoryOverflowModel());
	}

	private void releaseObservedChange() {
		synchronized (observations) {
			observations.clear();
			changes.clear();
		}
	}

	private Set<SailConnectionListener> getListeners() {
		synchronized (listeners) {
			if (listeners.isEmpty())
				return Collections.emptySet();
			return new HashSet<SailConnectionListener>(listeners);
		}
	}

	private ConcurrencyException getConcurrencyConflict() throws SailException {
		synchronized (observations) {
			if (changes.isEmpty())
				return null;
			if (isSerializable()) {
				ConcurrencyException c = changes.getFirst().getInconsistency();
				return new ConcurrencyException(c);
			}
			// check if the inconsistent state caused a phantom read
			return detectPhantomRead(changes);
		}
	}

	/**
	 * Searches for observations that observed a subsequent state change of the store
	 */
	private ConcurrencyException detectPhantomRead(LinkedList<InconsistentChange> changes) throws SailException {
		for (InconsistentChange cs : changes) {
			Model added = cs.getAdded();
			Model removed = cs.getRemoved();
			for (EvaluateOperation op : cs.getSubsequentObservations()) {
				if (!added.isEmpty() && sail.effects(added, op)) {
					return phantom(op, cs.getInconsistency());
				}
				if (!removed.isEmpty() && sail.effects(removed, op)) {
					return phantom(op, cs.getInconsistency());
				}
			}
		}
		return null;
	}

	private ConcurrencyException phantom(EvaluateOperation op, ConcurrencyException cause) {
		return new ConcurrencyException("Observed Inconsistent State", op,
				cause);
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
			Var ctxVar = new Var("graph", ctx);
			Scope scope = ctx == null ? DEFAULT_CONTEXTS : NAMED_CONTEXTS;
			TupleExpr sp = new StatementPattern(scope, subjVar, predVar,
					objVar, ctxVar);
			union = union == null ? sp : new Union(union, sp);
		}
		addRead(new EvaluateOperation(union, inf));
	}

	private void addRead(EvaluateOperation op) {
		if (isSnapshot()) {
			synchronized (observations) {
				observations.add(op);
				for (InconsistentChange changeset : changes) {
					changeset.addObservation(op);
				}
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
