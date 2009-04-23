package org.openrdf.sail.optimistic.helpers;

import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

public class LeftJoinDetector extends QueryModelVisitorBase<RuntimeException> {
	private QueryModelNode root;
	private boolean present;

	public LeftJoinDetector(QueryModelNode root) {
		this.root = root;
	}

	public boolean isPresent() {
		present = false;
		root.visit(this);
		return present;
	}

	@Override
	public void meet(LeftJoin node) {
		present = true;
	}

}
