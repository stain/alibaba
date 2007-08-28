package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.factories.PerspectiveFactoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Use to create perspectives as needed. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#PerspectiveFactory")
public interface PerspectiveFactory extends Thing, PerspectiveFactoryBehaviour {


	/** Factory used to create displays. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#displayFactory")
	public abstract DisplayFactory getPovDisplayFactory();

	/** Factory used to create displays. */
	public abstract void setPovDisplayFactory(DisplayFactory value);

}
