package org.openrdf.alibaba.pov;

import java.util.Set;


public interface ExpressionRepositoryBehaviour extends RepositoryBehaviour<Expression> {
	public abstract Set<Expression> findByNames(Set<String> names);
}
