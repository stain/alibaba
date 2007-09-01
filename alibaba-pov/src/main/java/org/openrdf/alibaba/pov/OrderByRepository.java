package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Repository of ORDER BY Expressions. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#OrderByRepository")
public interface OrderByRepository extends ExpressionRepository, OrderByRepositoryBehaviour {

}
