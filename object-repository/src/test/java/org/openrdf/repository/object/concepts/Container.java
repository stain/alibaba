package org.openrdf.repository.object.concepts;

import org.openrdf.annotations.iri;

/** The class of RDF containers. */
@iri("http://www.w3.org/2000/01/rdf-schema#Container")
public interface Container<E> extends java.util.List<E> {

}
