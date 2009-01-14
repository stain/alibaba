package org.openrdf.sail.optimistic.helpers;

import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.impl.EmptyBindingSet;

public class EvaluateOperation {
	private QueryModel qry;
	private BindingSet bindings = EmptyBindingSet.getInstance();;
	private boolean inf;

	public EvaluateOperation(QueryModel query, TupleExpr expr,
			BindingSet bindings, boolean inf) {
		this(expr, inf);
		qry.setDefaultGraphs(query.getDefaultGraphs());
		qry.setNamedGraphs(query.getNamedGraphs());
		this.bindings = bindings;
	}

	public EvaluateOperation(TupleExpr expr, boolean inf) {
		qry = new QueryModel(expr);
		this.inf = inf;
	}

	public QueryModel getQueryModel() {
		return qry;
	}

	public BindingSet getBindingSet() {
		return bindings;
	}

	public boolean isIncludeInferred() {
		return inf;
	}

}
