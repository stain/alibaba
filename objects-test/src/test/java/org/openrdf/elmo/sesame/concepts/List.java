package org.openrdf.elmo.sesame.concepts;

import org.openrdf.elmo.annotations.rdf;

/** The class of RDF Lists. */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
public interface List<E> extends java.util.List<E> {


	/** The first item in the subject RDF list. */
	@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
	public abstract E getRdfFirst();

	/** The first item in the subject RDF list. */
	public abstract void setRdfFirst(E value);


	/** The rest of the subject RDF list after the first item. */
	@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
	public abstract List<E> getRdfRest();

	/** The rest of the subject RDF list after the first item. */
	public abstract void setRdfRest(List<E> value);

}
