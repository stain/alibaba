package org.openrdf.alibaba.concepts;

import org.openrdf.elmo.annotations.rdf;

/** Description of how resources that should be retrived and shown. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#SearchDisplay")
public interface SearchDisplay extends Display {


	/** Specifies a SearchPattern to be used for retrived of values. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#searchPattern")
	public abstract SearchPattern getPovSearchPattern();

	/** Specifies a SearchPattern to be used for retrived of values. */
	public abstract void setPovSearchPattern(SearchPattern value);

}
