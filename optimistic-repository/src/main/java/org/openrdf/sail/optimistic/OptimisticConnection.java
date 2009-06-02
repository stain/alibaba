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

import static org.openrdf.query.algebra.StatementPattern.Scope.DEFAULT_CONTEXTS;
import static org.openrdf.query.algebra.StatementPattern.Scope.NAMED_CONTEXTS;
import info.aduna.concurrent.locks.Lock;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.UnionIteration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.DefaultSailChangedEvent;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencySailException;
import org.openrdf.sail.optimistic.helpers.BasicNodeCollector;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;

/**
 * Ensures every transaction has serializable isolation.
 * 
 * @author James Leigh
 *
 */
public class OptimisticConnection extends SailConnectionWrapper implements
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

	private static final int LARGE_BLOCK = 512;
	private OptimisticSail sail;
	private volatile boolean active;
	/** If no other transactions */
	private volatile boolean exclusive;
	/** locked by this */
	private Model added = new LinkedHashModel();
	/** locked by this */
	private Model removed = new LinkedHashModel();
	/** locked by sail.getReadLock() then this */
	private Set<EvaluateOperation> read = new HashSet<EvaluateOperation>();
	/** If sail.getWriteLock() */
	private volatile boolean prepared;
	private volatile ConcurrencyException invalid;
	private volatile DefaultSailChangedEvent event;
	private volatile boolean listenersIsEmpty = true;
	private Set<SailConnectionListener> listeners = new HashSet<SailConnectionListener>();

	public OptimisticConnection(OptimisticSail sail, SailConnection wrappedCon) {
		super(wrappedCon);
		this.sail = sail;
	}

	@Override
	public void close() throws SailException {
		if (active) {
			rollback();
		}
		super.close();
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

	void setConclict(ConcurrencyException exc) {
		this.invalid = exc;
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

	private Set<SailConnectionListener> getListeners() {
		synchronized (listeners) {
			if (listeners.isEmpty())
				return Collections.emptySet();
			return new HashSet<SailConnectionListener>(listeners);
		}
	}

	public boolean isAutoCommit() throws SailException {
		return !active;
	}

	public synchronized void begin() throws SailException {
		assert active == false;
		active = true;
		event = new DefaultSailChangedEvent(sail);
		read.clear();
		added.clear();
		removed.clear();
		try {
			sail.begin(this);
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
	}

	@Override
	public synchronized void commit() throws SailException {
		if (!prepared) {
			prepare();
		}
		if (!exclusive) {
			flush();
		}
		super.commit();
		active = false;
		prepared = false;
		sail.end(this);
		for (SailChangedListener listener : sail.getListeners()) {
			listener.sailChanged(event);
		}
		event = null;
	}

	@Override
	public synchronized void rollback() throws SailException {
		added.clear();
		removed.clear();
		read.clear();
		active = false;
		sail.end(this);
		event = null;
	}

	private synchronized void prepare() throws SailException {
		try {
			sail.prepare(this);
			prepared = true;
			read.clear();
		} catch (InterruptedException e) {
			if (invalid == null)
				throw new SailException(e);
		}
		if (invalid != null) {
			try {
				throw new ConcurrencySailException(invalid);
			} finally {
				rollback();
			}
		}
	}

	/** locked by this */
	void flush() throws SailException {
		for (Statement st : removed) {
			super.removeStatements(st.getSubject(), st.getPredicate(), st
					.getObject(), st.getContext());
		}
		removed.clear();
		for (Statement st : added) {
			super.addStatement(st.getSubject(), st.getPredicate(), st
					.getObject(), st.getContext());
		}
		added.clear();
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		AddOperation op = new AddOperation() {

			public int addLater(Resource subj, URI pred, Value obj,
					Resource... contexts) {
				if (removed.contains(subj, pred, obj, contexts)) {
					removed.remove(subj, pred, obj, contexts);
				} else {
					added.add(subj, pred, obj, contexts);
				}
				return added.size();
			}

			public void addNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				OptimisticConnection.super.addStatement(subj, pred, obj,
						contexts);
			}
		};
		add(op, subj, pred, obj, contexts);
	}

	@Override
	public void removeStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		RemoveOperation op = new RemoveOperation() {

			public int removeLater(Statement st) {
				added.remove(st);
				removed.add(st);
				return removed.size();
			}

			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws SailException {
				OptimisticConnection.super.removeStatements(subj, pred, obj,
						contexts);
			}
		};
		remove(op, subj, pred, obj, false, contexts);
	}

	void add(AddOperation op, Resource subj, URI pred, Value obj,
			Resource... contexts) throws SailException {
		if (exclusive) {
			op.addNow(subj, pred, obj, contexts);
		} else {
			synchronized (this) {
				int size = op.addLater(subj, pred, obj, contexts);
				if (listenersIsEmpty && size > 0 && size % LARGE_BLOCK == 0) {
					if (sail.exclusive(this)) {
						exclusive = true;
						read.clear();
						flush();
					}
				}
			}
		}
		event.setStatementsAdded(true);
		Resource[] ctxs = notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = new Resource[] { null };
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
			synchronized (this) {
				CloseableIteration<? extends Statement, SailException> stmts;
				stmts = super.getStatements(subj, pred, obj, inf, contexts);
				try {
					while (stmts.hasNext()) {
						called = true;
						int size = op.removeLater(stmts.next());
						if (listenersIsEmpty && size % LARGE_BLOCK == 0
								&& sail.exclusive(this)) {
							exclusive = true;
							read.clear();
							break;
						}
					}
				} finally {
					stmts.close();
				}
				if (exclusive) {
					flush();
					super.removeStatements(subj, pred, obj, contexts);
				}
			}
		}
		if (!listenersIsEmpty && called) {
			Set<SailConnectionListener> listeners = getListeners();
			CloseableIteration<? extends Statement, SailException> stmts;
			stmts = super.getStatements(subj, pred, obj, inf, contexts);
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

	@Override
	public long size(Resource... contexts) throws SailException {
		if (prepared)
			throw new IllegalStateException();
		long size = super.size(contexts);
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

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(
			Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) throws SailException {
		if (prepared)
			throw new IllegalStateException();
		CloseableIteration<? extends Statement, SailException> result;
		result = super.getStatements(subj, pred, obj, inf, contexts);
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
				return new UnionIteration<Statement, SailException>(incl,
						result);
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(
			TupleExpr query, Dataset dataset, BindingSet bindings, boolean inf)
			throws SailException {
		if (prepared)
			throw new IllegalStateException();
		if (!active || exclusive)
			return super.evaluate(query, dataset, bindings, inf);

		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				if (!added.isEmpty() || !removed.isEmpty()) {
					query = new QueryRoot(query.clone());
					DeltaMerger merger = new DeltaMerger(added, removed);
					merger.optimize(query, dataset, bindings);
				}

				BasicNodeCollector collector = new BasicNodeCollector(query);
				for (TupleExpr expr : collector.findBasicNodes()) {
					read
							.add(new EvaluateOperation(dataset, expr, bindings,
									inf));
				}
				return super.evaluate(query, dataset, bindings, inf);
			}
		} finally {
			lock.release();
		}
	}

	public Resource[] notNull(Resource[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
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
		read.add(new EvaluateOperation(union, inf));
	}
}
