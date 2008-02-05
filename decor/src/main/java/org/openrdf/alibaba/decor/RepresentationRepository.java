package org.openrdf.alibaba.decor;

import java.util.Set;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Set of representations with a common content-type. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#RepresentationRepository")
public interface RepresentationRepository extends Thing, RepresentationRepositoryBehaviour {


	/** Set of active representation in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#registeredRepresentation")
	public abstract Set<Representation> getPovRegisteredRepresentations();

	/** Set of active representation in repository. */
	public abstract void setPovRegisteredRepresentations(Set<Representation> value);

}
