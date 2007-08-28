package org.openrdf.alibaba.concepts;

import java.lang.String;
import org.openrdf.elmo.annotations.rdf;

/** Indicates how property values should be formatted. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#MessagePatternFormat")
public interface MessagePatternFormat extends Format {


	/** The printf pattern for values that should be used in this format. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#pattern")
	public abstract String getPovPattern();

	/** The printf pattern for values that should be used in this format. */
	public abstract void setPovPattern(String value);

}
