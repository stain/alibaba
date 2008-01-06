package org.openrdf.alibaba.pov;

/**
 * Methods used to access ORDER BY {@link Expression}s.
 * 
 * @author James Leigh
 *
 */
public interface OrderByRepositoryBehaviour extends
		ExpressionRepositoryBehaviour {
	public abstract Expression findByName(String name);

	public abstract OrderByExpression findAscending(Display display);

	public abstract OrderByExpression findDescending(Display display);
}
