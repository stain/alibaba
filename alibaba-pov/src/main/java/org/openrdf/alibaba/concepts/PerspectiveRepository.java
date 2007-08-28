package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.PerspectiveRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Aggregate repository of perspectives. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#PerspectiveRepository")
public interface PerspectiveRepository extends Thing, PerspectiveRepositoryBehaviour {


	/** Set of active perspectives in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredPerspective")
	public abstract Set<Perspective> getPovRegisteredPerspectives();

	/** Set of active perspectives in repository. */
	public abstract void setPovRegisteredPerspectives(Set<Perspective> value);

}
