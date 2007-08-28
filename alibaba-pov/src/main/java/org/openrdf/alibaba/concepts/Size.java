package org.openrdf.alibaba.concepts;

import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Contains the width and height restrictions of a display. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Size")
public interface Size extends Thing {


	/** The suggested height. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#height")
	public abstract Length getPovHeight();

	/** The suggested height. */
	public abstract void setPovHeight(Length value);


	/** The maximum height. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#maxHeight")
	public abstract Length getPovMaxHeight();

	/** The maximum height. */
	public abstract void setPovMaxHeight(Length value);


	/** The maximum width. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#maxWidth")
	public abstract Length getPovMaxWidth();

	/** The maximum width. */
	public abstract void setPovMaxWidth(Length value);


	/** The minimum height. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#minHeight")
	public abstract Length getPovMinHeight();

	/** The minimum height. */
	public abstract void setPovMinHeight(Length value);


	/** The minimum width. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#minWidth")
	public abstract Length getPovMinWidth();

	/** The minimum width. */
	public abstract void setPovMinWidth(Length value);


	/** The suggested width. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#width")
	public abstract Length getPovWidth();

	/** The suggested width. */
	public abstract void setPovWidth(Length value);

}
