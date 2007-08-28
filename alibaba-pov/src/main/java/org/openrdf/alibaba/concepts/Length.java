package org.openrdf.alibaba.concepts;

import java.lang.Integer;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.elmo.annotations.rdf;

/** Measurement of a width or height for a display. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Length")
public interface Length extends Thing {


	/** The x-height or corpus size refers to the distance between the baseline and the mean line in a typeface. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inCorpusSize")
	public abstract Integer getPovInCorpusSize();

	/** The x-height or corpus size refers to the distance between the baseline and the mean line in a typeface. */
	public abstract void setPovInCorpusSize(Integer value);


	/** Length in the display font height. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inFontSize")
	public abstract Integer getPovInFontSize();

	/** Length in the display font height. */
	public abstract void setPovInFontSize(Integer value);


	/** Lengith in pixels. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inPixels")
	public abstract Integer getPovInPixels();

	/** Lengith in pixels. */
	public abstract void setPovInPixels(Integer value);


	/** 1 pt is equal to 1/72nd of an inch. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#inPoints")
	public abstract Integer getPovInPoints();

	/** 1 pt is equal to 1/72nd of an inch. */
	public abstract void setPovInPoints(Integer value);

}
