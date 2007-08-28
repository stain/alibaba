package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.behaviours.SearchPatternBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Describes a query and how to display it. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#SearchPattern")
public interface SearchPattern extends Thing, PerspectiveOrSearchPattern, SearchPatternBehaviour {


	/** The FILTER or WHERE expression(s). */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#filterExpressions")
	public abstract ExpressionRepository getPovFilterExpressions();

	/** The FILTER or WHERE expression(s). */
	public abstract void setPovFilterExpressions(ExpressionRepository value);


	/** The query expression after the filters, including closing WHERE and GROUP BY. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#groupByExpression")
	public abstract Expression getPovGroupByExpression();

	/** The query expression after the filters, including closing WHERE and GROUP BY. */
	public abstract void setPovGroupByExpression(Expression value);


	/** The ORDER BY expression(s). */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#orderByExpressions")
	public abstract OrderByRepository getPovOrderByExpressions();

	/** The ORDER BY expression(s). */
	public abstract void setPovOrderByExpressions(OrderByRepository value);


	/** The primary query expression, including SELECT, FROM, and begin WHERE. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#selectExpression")
	public abstract Expression getPovSelectExpression();

	/** The primary query expression, including SELECT, FROM, and begin WHERE. */
	public abstract void setPovSelectExpression(Expression value);

}
