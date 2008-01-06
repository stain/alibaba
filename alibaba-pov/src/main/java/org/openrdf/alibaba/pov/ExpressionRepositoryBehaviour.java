package org.openrdf.alibaba.pov;

import java.util.Set;

import org.openrdf.alibaba.core.RepositoryBehaviour;

/**
 * Interface for the findByNames method.
 * 
 * @author James Leigh
 * 
 */
public interface ExpressionRepositoryBehaviour extends
		RepositoryBehaviour<Expression> {
	public abstract Set<Expression> findByNames(Set<String> names);
}
