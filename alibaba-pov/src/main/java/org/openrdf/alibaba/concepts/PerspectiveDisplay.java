package org.openrdf.alibaba.concepts;

import org.openrdf.elmo.annotations.rdf;

/** Description of how resources should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#PerspectiveDisplay")
public interface PerspectiveDisplay extends Display {


	/** Perspective which should be used for displaying the object values. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#perspective")
	public abstract Perspective getPovPerspective();

	/** Perspective which should be used for displaying the object values. */
	public abstract void setPovPerspective(Perspective value);

}
