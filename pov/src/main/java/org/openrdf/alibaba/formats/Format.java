package org.openrdf.alibaba.formats;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Describes how a value should appear. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#Format")
public interface Format extends Thing, FormatBehaviour {

}
