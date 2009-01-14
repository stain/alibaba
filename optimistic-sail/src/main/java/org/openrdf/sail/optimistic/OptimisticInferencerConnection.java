package org.openrdf.sail.optimistic;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.Cursor;
import org.openrdf.cursor.FilteringCursor;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ModelImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.evaluation.cursors.UnionCursor;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.optimistic.helpers.DeltaMerger;
import org.openrdf.store.StoreException;

public class OptimisticInferencerConnection extends OptimisticConnection
		implements InferencerConnection {
	private Model added = new ModelImpl();
	private Model removed = new ModelImpl();
	private InferencerConnection delegate;

	public OptimisticInferencerConnection(OptimisticSail sail,
			InferencerConnection delegate) {
		super(sail, delegate);
		this.delegate = delegate;
	}

	public boolean addInferredStatement(Resource subj, URI pred, Value obj,
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
				delegate.addInferredStatement(subj, pred, obj, contexts);
			}
		};
		add(op, subj, pred, obj, contexts);
		return true;
	}

	public boolean removeInferredStatement(Resource subj, URI pred, Value obj,
			Resource... contexts) throws StoreException {
		RemoveOperation op = new RemoveOperation() {

			public int removeLater(Statement st) {
				added.remove(st);
				removed.add(st);
				return removed.size();
			}

			public void removeNow(Resource subj, URI pred, Value obj,
					Resource... contexts) throws StoreException {
				delegate.removeInferredStatement(subj, pred, obj, contexts);
			}
		};
		return remove(op, subj, pred, obj, true, contexts);
	}

	public void flushUpdates() throws StoreException {
		// no-op
	}

	@Override
	public synchronized void begin() throws StoreException {
		added.clear();
		removed.clear();
		super.begin();
	}

	@Override
	public synchronized void rollback() throws StoreException {
		added.clear();
		removed.clear();
		super.rollback();
	}

	@Override
	void flush() throws StoreException {
		super.flush();
		for (Statement st : removed) {
			delegate.removeInferredStatement(st.getSubject(),
					st.getPredicate(), st.getObject(), st.getContext());
		}
		removed.clear();
		for (Statement st : added) {
			delegate.addInferredStatement(st.getSubject(), st.getPredicate(),
					st.getObject(), st.getContext());
		}
		added.clear();
	}

	@Override
	public long size(Resource subj, URI pred, Value obj, boolean inf,
			Resource... contexts) throws StoreException {
		long size = super.size(subj, pred, obj, inf, contexts);
		if (!isActive())
			return size;
		synchronized (this) {
			int rsize = removed.filter(subj, pred, obj, contexts).size();
			int asize = added.filter(subj, pred, obj, contexts).size();
			return size - rsize + asize;
		}
	}

	@Override
	public Cursor<? extends Statement> getStatements(Resource subj, URI pred,
			Value obj, boolean inf, Resource... contexts) throws StoreException {
		Cursor<? extends Statement> result;
		result = super.getStatements(subj, pred, obj, inf, contexts);
		synchronized (this) {
			if (!inf || !isActive() || added.isEmpty() && removed.isEmpty())
				return result;
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
	}

	@Override
	public synchronized Cursor<? extends BindingSet> evaluate(QueryModel qry,
			BindingSet bindings, boolean inf) throws StoreException {
		QueryModel query = qry;
		synchronized (this) {
			if (inf && isActive() && (!added.isEmpty() || !removed.isEmpty())) {
				query = query.clone();
				DeltaMerger merger = new DeltaMerger(added, removed, query);
				merger.optimize(query, bindings);
			}
		}
		return super.evaluate(query, bindings, inf);
	}

}
