package org.openrdf.sail.optimistic.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
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
	public void meet(StatementPattern node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		}
	}

	@Override
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		} else {
			super.meetBinaryTupleOperator(node);
		}
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		} else {
			super.meetUnaryTupleOperator(node);
		}
	}

	private boolean isBasic(QueryModelNode node) {
		return node instanceof TupleExpr && new BasicNodeJudge(node).isBasic();
	}

}
