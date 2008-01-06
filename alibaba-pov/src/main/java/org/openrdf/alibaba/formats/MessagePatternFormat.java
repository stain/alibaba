package org.openrdf.alibaba.formats;

import java.lang.String;
import org.openrdf.elmo.annotations.rdf;

/** Formatted base on a given pattern. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#MessagePatternFormat")
public interface MessagePatternFormat extends Format {


	/** The java.text.MessageFormat pattern for values that should be used in this format. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#pattern")
	public abstract String getPovPattern();

	/** The java.text.MessageFormat pattern for values that should be used in this format. */
	public abstract void setPovPattern(String value);

}
