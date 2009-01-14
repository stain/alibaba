package org.openrdf.elmo.sesame.concepts;

import org.openrdf.repository.object.annotations.rdf;

/** The class of ordered containers. */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq")
public interface Seq<E> extends Container<E> {

}
