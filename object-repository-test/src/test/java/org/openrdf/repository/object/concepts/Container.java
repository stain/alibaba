package org.openrdf.repository.object.concepts;

import org.openrdf.repository.object.annotations.rdf;

/** The class of RDF containers. */
@rdf("http://www.w3.org/2000/01/rdf-schema#Container")
public interface Container<E> extends java.util.List<E> {

}
