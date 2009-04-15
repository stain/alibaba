package org.openrdf.sail.optimistic.helpers;

import org.openrdf.query.algebra.BinaryValueOperator;
import org.openrdf.query.algebra.Difference;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.SingletonSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UnaryValueOperator;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

public class BasicNodeJudge extends QueryModelVisitorBase<RuntimeException> {
	private QueryModelNode root;
	private boolean basic;
	private boolean complex;
	private boolean leftJoinPresent;

	public BasicNodeJudge(QueryModelNode root) {
		this.root = root;
		leftJoinPresent = new LeftJoinDetector(root).isPresent();
	}

	public boolean isBasic() {
		boolean before = complex;
		try {
			complex = false;
			root.visit(this);
			return !complex;
		} finally {
			complex = before;
		}
	}

	/**
	 * Only basic if right side is constant.
	 */
	@Override
	public void meet(Difference node) throws RuntimeException {
		basic = node.getRightArg() instanceof ExternalSet;
		super.meet(node);
	}

	@Override
	public void meet(EmptySet node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	/**
	 * Only basic if there is no OPTIONAL join.
	 */
	@Override
	public void meet(Filter node) throws RuntimeException {
		basic = !leftJoinPresent;
		super.meet(node);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	/**
	 * Only basic if there is no condition.
	 */
	@Override
	public void meet(LeftJoin node) throws RuntimeException {
		basic = !node.hasCondition();
		super.meet(node);
	}

	@Override
	public void meet(SingletonSet node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(Union node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	protected void meetBinaryValueOperator(BinaryValueOperator node) {
		basic = true;
		super.meetBinaryValueOperator(node);
	}

	@Override
	protected void meetUnaryValueOperator(UnaryValueOperator node) {
		basic = true;
		super.meetUnaryValueOperator(node);
	}

	@Override
	protected void meetNode(QueryModelNode node) throws RuntimeException {
		if (basic) {
			basic = false;
			node.visitChildren(this);
		} else {
			complex = true;
		}
	}

}
