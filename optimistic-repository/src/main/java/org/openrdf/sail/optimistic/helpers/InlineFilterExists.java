package org.openrdf.sail.optimistic.helpers;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.Exists;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

public class InlineFilterExists extends QueryModelVisitorBase<RuntimeException> implements QueryOptimizer {
	private boolean readOnly;
	private boolean present;

	public synchronized boolean isPresent(QueryModelNode root) {
		readOnly = true;
		present = false;
		root.visit(this);
		return present;
	}

	public synchronized void optimize(TupleExpr tupleExpr, Dataset dataset,
			BindingSet bindings) {
		readOnly = false;
		present = false;
		tupleExpr.visit(this);
	}

	@Override
	public void meet(Filter node) throws RuntimeException {
		super.meet(node);
		if (node.getCondition() instanceof Exists) {
			Exists exists = (Exists) node.getCondition();
			if (!readOnly) {
				node.replaceWith(new Join(node.getArg(), exists.getSubQuery()));
			}
			present = true;
		}
	}

}
