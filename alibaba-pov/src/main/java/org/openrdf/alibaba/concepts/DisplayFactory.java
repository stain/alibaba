package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.factories.DisplayFactoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Used to create displays as needed. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#DisplayFactory")
public interface DisplayFactory extends Thing, DisplayFactoryBehaviour {

}
