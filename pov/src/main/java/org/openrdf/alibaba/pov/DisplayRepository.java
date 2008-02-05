package org.openrdf.alibaba.pov;

import java.util.Set;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Repository of active displays. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#DisplayRepository")
public interface DisplayRepository extends Thing, DisplayRepositoryBehaviour {


	/** Set of active displays in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#registeredDisplay")
	public abstract Set<Display> getPovRegisteredDisplays();

	/** Set of active displays in repository. */
	public abstract void setPovRegisteredDisplays(Set<Display> value);

}
