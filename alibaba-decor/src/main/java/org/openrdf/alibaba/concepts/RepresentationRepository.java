package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.RepresentationRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Set of representations with a common content-type. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#RepresentationRepository")
public interface RepresentationRepository extends Thing, RepresentationRepositoryBehaviour {


	/** Set of active representation in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredRepresentation")
	public abstract Set<Representation> getPovRegisteredRepresentations();

	/** Set of active representation in repository. */
	public abstract void setPovRegisteredRepresentations(Set<Representation> value);

}
