package org.openrdf.alibaba.repositories;

import java.util.Set;

import org.openrdf.alibaba.concepts.Expression;

public interface ExpressionRepositoryBehaviour extends RepositoryBehaviour<Expression> {
	public abstract Set<Expression> findByNames(Set<String> names);
}
