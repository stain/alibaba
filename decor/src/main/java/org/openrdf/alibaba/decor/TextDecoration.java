package org.openrdf.alibaba.decor;

import org.openrdf.elmo.annotations.rdf;

/** Decortation of static text used by presentations and representations. */
@rdf("http://www.openrdf.org/rdf/2007/09/decor#TextDecoration")
public interface TextDecoration extends Decoration {


	/** Text to include after the value. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#after")
	public abstract String getPovAfter();

	/** Text to include after the value. */
	public abstract void setPovAfter(String value);


	/** Text to include before the value. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#before")
	public abstract String getPovBefore();

	/** Text to include before the value. */
	public abstract void setPovBefore(String value);


	/** Text to use if no value exists. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#empty")
	public abstract String getPovEmpty();

	/** Text to use if no value exists. */
	public abstract void setPovEmpty(String value);


	/** Text to include between values. */
	@rdf("http://www.openrdf.org/rdf/2007/09/decor#separation")
	public abstract String getPovSeparation();

	/** Text to include between values. */
	public abstract void setPovSeparation(String value);

}
