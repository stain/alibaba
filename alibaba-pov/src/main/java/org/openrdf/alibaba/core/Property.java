package org.openrdf.alibaba.core;

import org.openrdf.elmo.annotations.rdf;

/** The class of RDF properties. */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface Property extends org.openrdf.concepts.rdf.Property,
		PropertyValueBehaviour {

}
