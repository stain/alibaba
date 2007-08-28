package org.openrdf.alibaba.concepts;

import org.openrdf.elmo.annotations.rdf;

/** Description of how resources should be formatted and shown. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#LiteralDisplay")
public interface LiteralDisplay extends Display {


	/** The format that should be used for displaying the literal values. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#format")
	public abstract Format getPovFormat();

	/** The format that should be used for displaying the literal values. */
	public abstract void setPovFormat(Format value);

}
