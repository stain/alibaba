package org.openrdf.alibaba.factories;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Use to create dynamic perspectives as needed. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#PerspectiveFactory")
public interface PerspectiveFactory extends Thing, PerspectiveFactoryBehaviour {


	/** Factory used to create displays. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#displayFactory")
	public abstract DisplayFactory getPovDisplayFactory();

	/** Factory used to create displays. */
	public abstract void setPovDisplayFactory(DisplayFactory value);

}
