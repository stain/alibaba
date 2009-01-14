package org.openrdf.sail.optimistic.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.query.algebra.NaryTupleOperator;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

public class BasicNodeCollector extends QueryModelVisitorBase<RuntimeException> {
	private List<TupleExpr> list;
	private TupleExpr root;

	public BasicNodeCollector(TupleExpr root) {
		this.root = root;
	}

	public List<TupleExpr> findBasicNodes() {
		if (isBasic(root))
			return Collections.singletonList(root);
		list = new ArrayList<TupleExpr>();
		root.visit(this);
		return list;
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		}
	}

	@Override
	protected void meetNaryTupleOperator(NaryTupleOperator node)
			throws RuntimeException {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		} else {
			super.meetNaryTupleOperator(node);
		}
	}

	private boolean isBasic(QueryModelNode node) {
		return node instanceof TupleExpr && new BasicNodeJudge(node).isBasic();
	}

}
