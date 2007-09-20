package org.openrdf.alibaba.pov;

import org.openrdf.elmo.annotations.rdf;

/** Display reference perspective for referencing a resource, identified by one or more of it classes. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#ReferencePerspective")
public interface ReferencePerspective extends Perspective {


	/** Search pattern that can be used to lookup the referenced resource. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#lookup")
	public abstract SearchPattern getPovLookup();

	/** Search pattern that can be used to lookup the referenced resource. */
	public abstract void setPovLookup(SearchPattern value);


	/** Search pattern that can be used to suggest resources from partial information. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#suggestion")
	public abstract SearchPattern getPovSuggestion();

	/** Search pattern that can be used to suggest resources from partial information. */
	public abstract void setPovSuggestion(SearchPattern value);

}
