package org.openrdf.repository.object.concepts;

import java.util.Set;

import org.openrdf.annotations.iri;

/** The class of classes. */
@iri("http://www.w3.org/2000/01/rdf-schema#Class")
public interface ClassConcept {


	/** The subject is a subclass of a class. */
	@iri("http://www.w3.org/2000/01/rdf-schema#subClassOf")
	public abstract Set<ClassConcept> getRdfsSubClassOf();

	/** The subject is a subclass of a class. */
	public abstract void setRdfsSubClassOf(Set<ClassConcept> value);

}
