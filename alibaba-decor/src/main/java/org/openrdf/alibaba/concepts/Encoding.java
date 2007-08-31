package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.behaviours.EncodingBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Encoding used to insert values into a representation. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#Encoding")
public interface Encoding extends Thing, EncodingBehaviour {

}
