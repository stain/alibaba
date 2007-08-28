package org.openrdf.alibaba.concepts;

import java.util.Set;
import org.openrdf.elmo.annotations.rdf;

/** Describes an ORDER BY expression for a display. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#OrderByExpression")
public interface OrderByExpression extends Expression {


	/** The display this expression is ordered by ascending */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#ascending")
	public abstract Set<Display> getPovAscendings();

	/** The display this expression is ordered by ascending */
	public abstract void setPovAscendings(Set<Display> value);


	/** The display this expression is ordered by descending */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#descending")
	public abstract Set<Display> getPovDescendings();

	/** The display this expression is ordered by descending */
	public abstract void setPovDescendings(Set<Display> value);

}
