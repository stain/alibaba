package org.openrdf.sail.optimistic.helpers;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.impl.EmptyBindingSet;

public class EvaluateOperation {
	private TupleExpr qry;
	private Dataset dataset;
	private BindingSet bindings = EmptyBindingSet.getInstance();;
	private boolean inf;

	public EvaluateOperation(Dataset dataset, TupleExpr expr,
			BindingSet bindings, boolean inf) {
		this(expr, inf);
		this.dataset = dataset;
		this.bindings = bindings;
	}

	public EvaluateOperation(TupleExpr expr, boolean inf) {
		qry = expr;
		this.inf = inf;
	}

	public TupleExpr getTupleExpr() {
		return qry;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public BindingSet getBindingSet() {
		return bindings;
	}

	public boolean isIncludeInferred() {
		return inf;
	}

}
