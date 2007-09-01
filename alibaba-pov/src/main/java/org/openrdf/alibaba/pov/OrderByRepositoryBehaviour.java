package org.openrdf.alibaba.pov;


public interface OrderByRepositoryBehaviour extends
		ExpressionRepositoryBehaviour {
	public abstract Expression findByName(String name);

	public abstract OrderByExpression findAscending(Display display);

	public abstract OrderByExpression findDescending(Display display);
}
