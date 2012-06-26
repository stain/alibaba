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

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadPrefReadWriteLockManager;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;
import info.aduna.iteration.CloseableIteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailChangedEvent;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailWrapper;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.sail.optimistic.helpers.EvaluateOperation;

/**
 * When used with the {@link OptimisticRepository}, transactions are concurrent
 * and may enforce snapshot and/or serializable isolation.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public class OptimisticSail extends SailWrapper implements NotifyingSail {

	private static final String DELTA_VARNAME = "-delta-";
	private boolean readSnapshot = true;
	private boolean snapshot;
	private boolean serializable;
	private ReadWriteLockManager preparing = new WritePrefReadWriteLockManager();
	private ReadWriteLockManager open = new ReadPrefReadWriteLockManager();
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

	/**
	 * @return <code>true</code> if read operations will operate on a single
	 *         state of the store.
	 */
	public boolean isReadSnapshot() {
		return readSnapshot;
	}

	public void setReadSnapshot(boolean readSnapshot) {
		this.readSnapshot = readSnapshot;
	}

	/**
	 * @return <code>true</code> if this connection will fail to commit when
	 *         multiple states are observed.
	 */
	public boolean isSnapshot() {
		return snapshot;
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	/**
	 * @return <code>true</code> if this connection will fail to commit when the
	 *         observed state has changed.
	 */
	public boolean isSerializable() {
		return serializable;
	}

	public void setSerializable(boolean serializable) {
		this.serializable = serializable;
	}

	@Override
	public String toString() {
		return getBaseSail().toString();
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		if (serializable) {
			snapshot = true;
		}
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
	public OptimisticConnection getConnection() throws SailException {
		SailConnection con = super.getConnection();
		OptimisticConnection optimistic = new OptimisticConnection(this, con);
		try {
			optimistic.setReadSnapshot(isReadSnapshot());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException(e);
		}
		optimistic.setSnapshot(isSnapshot());
		optimistic.setSerializable(isSerializable());
		return optimistic;
	}

	boolean isListenerPresent() {
		return !listenersIsEmpty;
	}

	void begin(OptimisticConnection con) throws InterruptedException {
		Lock lock = open.getReadLock();
		synchronized (this) {
			assert !transactions.containsKey(con);
			transactions.put(con, lock);
		}
	}

	Lock getReadLock() throws SailException {
		try {
			return preparing.getReadLock();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException(e);
		}
	}

	synchronized void prepare(OptimisticConnection prepared)
			throws InterruptedException, SailException {
		while (preparedLock != null && preparedLock.isActive()) {
			wait();
		}
		assert transactions.containsKey(prepared);
		preparedLock = preparing.getWriteLock();
		this.prepared = prepared;
		synchronized (prepared) {
			Model added = prepared.getAddedModel();
			Model removed = prepared.getRemovedModel();
			if (added.isEmpty() && removed.isEmpty())
				return;
			for (OptimisticConnection con : transactions.keySet()) {
				if (con == prepared)
					continue;
				con.changed(added, removed);
			}		
		}
	}

	void endAndNotify(OptimisticConnection con, SailChangedEvent event) {
		end(con);
		if (!isListenerPresent())
			return;
		// notify listeners (including named queries)
		// added and removed are empty if the connection is exclusive
		Set<SailChangedListener> copy;
		synchronized (listeners) {
			copy = new HashSet<SailChangedListener>(listeners);
		}
		for (SailChangedListener listener : copy) {
			listener.sailChanged(event);
		}
	}

	synchronized void end(OptimisticConnection con) {
		Lock lock = transactions.get(con);
		if (lock == null)
			return; // no active transaction
		try {
			transactions.remove(con);
			if (prepared == con) {
				preparedLock.release();
				prepared = null;
				notify();
			}
		} finally {
			lock.release();
		}
	}

	void exclusive(OptimisticConnection con) throws InterruptedException {
		Lock exclusive;
		synchronized (this) {
			transactions.remove(con).release();
		}
		exclusive = open.getWriteLock();
		synchronized (this) {
			end(con);
			transactions.put(con, exclusive);
		}
	}

	/**
	 * Modify the query to include the delta with an extra binding and see if
	 * the extra binding comes out the other side.
	 */
	boolean effects(Model delta, EvaluateOperation op) throws SailException {
		TupleExpr query = new QueryRoot(op.getTupleExpr().clone());
		BindingSet bindings = op.getBindingSet();
		boolean inf = op.isIncludeInferred();
	
		ValueFactory vf = getValueFactory();
		QueryBindingSet additional = new QueryBindingSet();
		additional.addBinding(DELTA_VARNAME, vf.createLiteral(true));
		DeltaMerger merger = new DeltaMerger(delta, additional);
		merger.optimize(query, op.getDataset(), bindings);
		if (!merger.isModified())
			return false;
	
		// open local connection for this transaction if required
		SailConnection localCon = super.getConnection() ;
		try {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> result;
			result = localCon.evaluate(query, op.getDataset(), bindings, inf);
			try {
				try {
					while (result.hasNext()) {
						if (result.next().hasBinding(DELTA_VARNAME))
							return true;
					}
					return false;
				} finally {
					result.close();
				}
			} catch (QueryEvaluationException e) {
				throw new SailException(e);
			}
		} finally {
			localCon.close();
		}
	}
	
}
