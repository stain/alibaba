package org.openrdf.script.model;

import java.util.List;

import org.openrdf.query.algebra.NaryOperator;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.QueryModelVisitor;

public class Body extends NaryOperator<QueryModelNode> {
	private static final long serialVersionUID = 6945188326658516433L;

	public Body(List<QueryModelNode> list) {
		super(list);
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

}
