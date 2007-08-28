package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.FormatRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Repository of active formats. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#FormatRepository")
public interface FormatRepository extends Thing, FormatRepositoryBehaviour {


	/** Set of active formats in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredFormat")
	public abstract Set<Format> getPovRegisteredFormats();

	/** Set of active formats in repository. */
	public abstract void setPovRegisteredFormats(Set<Format> value);

}
