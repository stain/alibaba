package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.repositories.OrderByRepositoryBehaviour;
import org.openrdf.elmo.annotations.rdf;

/** Repository of ORDER BY Expressions. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#OrderByRepository")
public interface OrderByRepository extends ExpressionRepository, OrderByRepositoryBehaviour {

}
