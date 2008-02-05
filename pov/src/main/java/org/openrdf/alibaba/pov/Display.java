package org.openrdf.alibaba.pov;

import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Size;
import org.openrdf.alibaba.formats.Style;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Description of how a value should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#Display")
public interface Display extends Thing, DisplayOrExpression, DisplayBehaviour {


	/** The width and height that should be used to display the values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#size")
	public abstract Size getPovSize();

	/** The width and height that should be used to display the values. */
	public abstract void setPovSize(Size value);


	/** The style that should be used for this display. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#style")
	public abstract Style getPovStyle();

	/** The style that should be used for this display. */
	public abstract void setPovStyle(Style value);


	/** The format that should be used for displaying the literal values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#format")
	public abstract Format getPovFormat();

	/** The format that should be used for displaying the literal values. */
	public abstract void setPovFormat(Format value);


	/** Perspective which should be used for displaying the object values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#perspective")
	public abstract Perspective getPovPerspective();

	/** Perspective which should be used for displaying the object values. */
	public abstract void setPovPerspective(Perspective value);


	/** Specifies a SearchPattern to be used for retrieved of values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#searchPattern")
	public abstract SearchPattern getPovSearchPattern();

	/** Specifies a SearchPattern to be used for retrieved of values. */
	public abstract void setPovSearchPattern(SearchPattern value);

}
