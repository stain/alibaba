package org.openrdf.alibaba.repositories;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.Expression;
import org.openrdf.alibaba.concepts.OrderByExpression;

public interface OrderByRepositoryBehaviour extends
		ExpressionRepositoryBehaviour {
	public abstract Expression findByName(String name);

	public abstract OrderByExpression findAscending(Display display);

	public abstract OrderByExpression findDescending(Display display);
}
