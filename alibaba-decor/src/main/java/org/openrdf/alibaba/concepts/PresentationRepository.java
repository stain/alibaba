package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.PresentationRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Aggregate presentations. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#PresentationRepository")
public interface PresentationRepository extends Thing, PresentationRepositoryBehaviour {


	/** Set of active presentations in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredPresentation")
	public abstract Set<Presentation> getPovRegisteredPresentations();

	/** Set of active presentations in repository. */
	public abstract void setPovRegisteredPresentations(Set<Presentation> value);

}
