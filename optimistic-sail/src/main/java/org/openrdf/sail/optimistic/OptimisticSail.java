package org.openrdf.sail.optimistic;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Model;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.helpers.AutoCommitNotifyingConnection;
import org.openrdf.sail.helpers.SailWrapper;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.inferencer.helpers.AutoCommitInferencerConnection;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;
import org.openrdf.store.StoreException;

public class OptimisticSail extends SailWrapper implements NotifyingSail {

	private static final String DELTA_VARNAME = "-delta-";
	private ReadWriteLockManager preparing = new WritePrefReadWriteLockManager();
	private ReadWriteLockManager locker = new WritePrefReadWriteLockManager();
	private Map<OptimisticConnection, Lock> transactions = new HashMap<OptimisticConnection, Lock>();
	private volatile Lock preparedLock;
	private volatile OptimisticConnection prepared;
	private volatile boolean listenersIsEmpty = true;
	private Set<SailChangedListener> listeners = new HashSet<SailChangedListener>();

	public OptimisticSail() {
		super();
	}

	public OptimisticSail(Sail baseSail) {
		super(baseSail);
	}

	public void addSailChangedListener(SailChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
			listenersIsEmpty = false;
		}
	}

	public void removeSailChangedListener(SailChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	@Override
	public NotifyingSailConnection getConnection() throws StoreException {
		SailConnection con = super.getConnection();
		if (con instanceof InferencerConnection) {
			return new AutoCommitInferencerConnection(
					new OptimisticInferencerConnection(this,
							(InferencerConnection) con));
		} else {
			return new AutoCommitNotifyingConnection(new OptimisticConnection(
					this, con));
		}
	}

	Set<SailChangedListener> getListeners() {
		if (listenersIsEmpty)
			return Collections.emptySet();
		synchronized (listeners) {
			return new HashSet<SailChangedListener>(listeners);
		}
	}

	synchronized void begin(OptimisticConnection con)
			throws InterruptedException {
		transactions.put(con, locker.getReadLock());
	}

	Lock getReadLock() throws StoreException {
		try {
			return preparing.getReadLock();
		} catch (InterruptedException e) {
			throw new StoreException(e);
		}
	}

	synchronized void prepare(OptimisticConnection prepared)
			throws InterruptedException, StoreException {
		assert transactions.containsKey(prepared);
		preparedLock = preparing.getWriteLock();
		this.prepared = prepared;
		synchronized (prepared) {
			Model added = prepared.getAddedModel();
			Model removed = prepared.getRemovedModel();
			SailConnection sail = super.getConnection();
			try {
				for (OptimisticConnection con : transactions.keySet()) {
					if (con == prepared)
						continue;
					synchronized (con) {
						for (EvaluateOperation op : con.getReadOperations()) {
							if (!added.isEmpty() && effects(added, op, sail)) {
								con.setConclict(new ConcurrencyException());
								break;
							}
							if (!removed.isEmpty()
									&& effects(removed, op, sail)) {
								con.setConclict(new ConcurrencyException());
								break;
							}
						}
					}
				}
			} finally {
				sail.close();
			}
		}
	}

	synchronized void end(OptimisticConnection con) {
		Lock lock = transactions.get(con);
		try {
			transactions.remove(con);
			if (prepared == con) {
				preparedLock.release();
			}
			con.setConclict(null);
		} finally {
			lock.release();
		}
	}

	synchronized boolean exclusive(OptimisticConnection con) {
		if (transactions.size() == 1 && transactions.containsKey(con)) {
			end(con);
			try {
				transactions.put(con, locker.getWriteLock());
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			return true;
		}
		return false;
	}

	/**
	 * Modify the query to include the delta with an extra binding and see if
	 * the extra binding comes out the other side.
	 */
	private boolean effects(Model delta, EvaluateOperation op,
			SailConnection sail) throws StoreException {
		QueryModel query = op.getQueryModel().clone();
		BindingSet bindings = op.getBindingSet();
		boolean inf = op.isIncludeInferred();

		ValueFactory vf = sail.getValueFactory();
		QueryBindingSet additional = new QueryBindingSet();
		additional.addBinding(DELTA_VARNAME, vf.createLiteral(true));
		DeltaMerger merger = new DeltaMerger(delta, query, additional);

		merger.optimize(query, bindings);
		if (!merger.isModified())
			return false;

		Cursor<? extends BindingSet> result;
		result = sail.evaluate(query, bindings, inf);
		try {
			BindingSet bs;
			while ((bs = result.next()) != null) {
				if (bs.hasBinding(DELTA_VARNAME))
					return true;
			}
			return false;
		} finally {
			result.close();
		}
	}
}
