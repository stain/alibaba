package org.openrdf.alibaba.pov;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Display perspective for a resource identified by one or more of it classes. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#Perspective")
public interface Perspective extends Thing, PerspectiveOrSearchPattern {

}
