package org.openrdf.alibaba.concepts;

import java.util.Set;

import org.openrdf.alibaba.repositories.SearchPatternRepositoryBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Repository of search patterns */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#SearchPatternRepository")
public interface SearchPatternRepository extends Thing, SearchPatternRepositoryBehaviour {


	/** Set of active search patterns in repository. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#registeredSearchPattern")
	public abstract Set<SearchPattern> getPovRegisteredSearchPatterns();

	/** Set of active search patterns in repository. */
	public abstract void setPovRegisteredSearchPatterns(Set<SearchPattern> value);

}
