package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.formats.FormatBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Describes how a resource should appear. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Format")
public interface Format extends Thing, FormatBehaviour {

}
