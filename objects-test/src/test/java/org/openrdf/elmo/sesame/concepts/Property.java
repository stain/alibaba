package org.openrdf.elmo.sesame.concepts;

import java.util.Set;

import org.openrdf.elmo.annotations.rdf;

/** The class of RDF properties. */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface Property {


	/** The subject is an instance of a class. */
	@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
	public abstract Set<ClassConcept> getRdfTypes();

	/** The subject is an instance of a class. */
	public abstract void setRdfTypes(Set<ClassConcept> value);


	/** A domain of the subject property. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#domain")
	public abstract Set<ClassConcept> getRdfsDomains();

	/** A domain of the subject property. */
	public abstract void setRdfsDomains(Set<ClassConcept> value);


	/** A range of the subject property. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#range")
	public abstract Set<ClassConcept> getRdfsRanges();

	/** A range of the subject property. */
	public abstract void setRdfsRanges(Set<ClassConcept> value);


	/** The subject is a subproperty of a property. */
	@rdf("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
	public abstract Set<Property> getRdfsSubPropertyOf();

	/** The subject is a subproperty of a property. */
	public abstract void setRdfsSubPropertyOf(Set<Property> value);

}
