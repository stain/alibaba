package org.openrdf.sail.optimistic;

import static org.openrdf.query.algebra.StatementPattern.Scope.DEFAULT_CONTEXTS;
import static org.openrdf.query.algebra.StatementPattern.Scope.NAMED_CONTEXTS;
import info.aduna.concurrent.locks.Lock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.OpenRDFUtil;
import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.Cursor;
import org.openrdf.cursor.FilteringCursor;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ModelImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.query.algebra.evaluation.cursors.UnionCursor;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.helpers.DefaultSailChangedEvent;
import org.openrdf.sail.helpers.SailConnectionWrapper;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.helpers.BasicNodeCollector;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.openrdf.store.StoreException;

public class OptimisticConnection extends SailConnectionWrapper implements
		NotifyingSailConnection {
	interface AddOperation {
		void addNow(Resource subj, URI pred, Value obj, Resource... contexts)
				throws StoreException;

		/** locked by this */
		int addLater(Resource subj, URI pred, Value obj, Resource... contexts);
	}

	interface RemoveOperation {
		void removeNow(Resource subj, URI pred, Value obj, Resource... contexts)
				throws StoreException;

		/** locked by this */
		int removeLater(Statement st);
	}

	private static final int LARGE_BLOCK = 512;
	private OptimisticSail sail;
	private volatile boolean active;
	/** If no other transactions */
	private volatile boolean exclusive;
	/** locked by this */
	private Model added = new ModelImpl();
	/** locked by this */
	private Model removed = new ModelImpl();
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
	public void close() throws StoreException {
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

	@Override
	public boolean isActive() throws StoreException {
		return active;
	}

	@Override
	public synchronized void begin() throws StoreException {
		assert active == false;
		active = true;
		event = new DefaultSailChangedEvent(sail);
		read.clear();
		added.clear();
		removed.clear();
		try {
			sail.begin(this);
		} catch (InterruptedException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public synchronized void commit() throws StoreException {
		if (!prepared) {
			prepare();
		}
		if (!exclusive) {
			super.begin();
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
	public synchronized void rollback() throws StoreException {
		added.clear();
		removed.clear();
		read.clear();
		active = false;
		sail.end(this);
		event = null;
	}

	private synchronized void prepare() throws StoreException {
		try {
			sail.prepare(this);
			prepared = true;
			read.clear();
		} catch (InterruptedException e) {
			if (invalid == null)
				throw new StoreException(e);
		}
		if (invalid != null) {
			try {
				throw new ConcurrencyException(invalid);
			} finally {
				rollback();
			}
		}
	}

	/** locked by this */
	void flush() throws StoreException {
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
			Resource... contexts) throws StoreException {
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
					Resource... contexts) throws StoreException {
				OptimisticConnection.super.addStatement(subj, pred, obj,
						contexts);
			}
		};
		add(op, subj, pred, obj, contexts);
	}

	@Override
	public void removeStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws StoreException {
		RemoveOperation op = new RemoveOperation() {

			public int removeLater(Statement st) {
				added.remove(st);
				removed.add(st);
				return removed.size();
			}

			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws StoreException {
				OptimisticConnection.super.removeStatements(subj, pred, obj,
						contexts);
			}
		};
		remove(op, subj, pred, obj, false, contexts);
	}

	void add(AddOperation op, Resource subj, URI pred, Value obj,
			Resource... contexts) throws StoreException {
		if (exclusive) {
			op.addNow(subj, pred, obj, contexts);
		} else {
			synchronized (this) {
				int size = op.addLater(subj, pred, obj, contexts);
				if (listenersIsEmpty && size % LARGE_BLOCK == 0) {
					if (sail.exclusive(this)) {
						exclusive = true;
						read.clear();
						super.begin();
						flush();
					}
				}
			}
		}
		event.setStatementsAdded(true);
		Resource[] ctxs = OpenRDFUtil.notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = new Resource[] { null };
		}
		for (SailConnectionListener listener : getListeners()) {
			for (Resource ctx : ctxs) {
				Statement st = new StatementImpl(subj, pred, obj, ctx);
				listener.statementAdded(st);
			}
		}
	}

	/**
	 * @return If RemoveOperation was called
	 */
	boolean remove(RemoveOperation op, Resource subj, URI pred, Value obj,
			boolean inf, Resource... contexts) throws StoreException {
		boolean called = false;
		event.setStatementsRemoved(true);
		if (exclusive) {
			called = true;
			op.removeNow(subj, pred, obj, contexts);
		} else {
			synchronized (this) {
				Statement st;
				Cursor<? extends Statement> stmts;
				stmts = super.getStatements(subj, pred, obj, inf, contexts);
				try {
					while ((st = stmts.next()) != null) {
						called = true;
						int size = op.removeLater(st);
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
					super.begin();
					flush();
					super.removeStatements(subj, pred, obj, contexts);
				}
			}
		}
		if (!listenersIsEmpty && called) {
			Set<SailConnectionListener> listeners = getListeners();
			Statement st;
			Cursor<? extends Statement> stmts;
			stmts = super.getStatements(subj, pred, obj, inf, contexts);
			try {
				while ((st = stmts.next()) != null) {
					for (SailConnectionListener listener : listeners) {
						listener.statementRemoved(st);
					}
				}
			} finally {
				stmts.close();
			}
		}
		return called;
	}

	@Override
	public long size(Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) throws StoreException {
		if (prepared)
			throw new IllegalStateException();
		long size = super.size(subj, pred, obj, inf, contexts);
		if (!active || exclusive)
			return size;
		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				read(subj, pred, obj, inf, contexts);
				int rsize = removed.filter(subj, pred, obj, contexts).size();
				int asize = added.filter(subj, pred, obj, contexts).size();
				return size - rsize + asize;
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public Cursor<? extends Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean inf, Resource... contexts) throws StoreException {
		if (prepared)
			throw new IllegalStateException();
		Cursor<? extends Statement> result;
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
					result = new FilteringCursor<Statement>(result) {
						@Override
						protected boolean accept(Statement stmt)
								throws StoreException {
							return !set.contains(stmt);
						}
					};
				}
				HashSet<Statement> set = new HashSet<Statement>(included);
				Cursor<Statement> incl = new CollectionCursor<Statement>(set);
				return new UnionCursor<Statement>(incl, result);
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public synchronized Cursor<? extends BindingSet> evaluate(QueryModel query,
			BindingSet bindings, boolean inf) throws StoreException {
		if (prepared)
			throw new IllegalStateException();
		if (!active || exclusive)
			return super.evaluate(query, bindings, inf);

		Lock lock = sail.getReadLock();
		try {
			synchronized (this) {
				if (!added.isEmpty() || !removed.isEmpty()) {
					query = query.clone();
					DeltaMerger merger = new DeltaMerger(added, removed, query);
					merger.optimize(query, bindings);
				}

				BasicNodeCollector collector = new BasicNodeCollector(query);
				for (TupleExpr expr : collector.findBasicNodes()) {
					read.add(new EvaluateOperation(query, expr, bindings, inf));
				}
				return super.evaluate(query, bindings, inf);
			}
		} finally {
			lock.release();
		}
	}

	/** locked by this */
	private void read(Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) {
		contexts = OpenRDFUtil.notNull(contexts);
		Var subjVar = new Var("subj", subj);
		Var predVar = new Var("pred", pred);
		Var objVar = new Var("obj", obj);
		Union union = new Union();
		if (contexts.length == 0) {
			union.addArg(new StatementPattern(subjVar, predVar, objVar));
		}
		for (Resource ctx : contexts) {
			Var ctxVar = new Var("ctx", ctx);
			Scope scope = ctx == null ? DEFAULT_CONTEXTS : NAMED_CONTEXTS;
			union.addArg(new StatementPattern(scope, subjVar, predVar, objVar,
					ctxVar));
		}
		read.add(new EvaluateOperation(union, inf));
	}
}
