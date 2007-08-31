package org.openrdf.alibaba.concepts;

import java.lang.String;
import org.openrdf.elmo.annotations.rdf;

public interface DisplayOrExpression  {


	/** Binding name used within query. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#name")
	public abstract String getPovName();

	/** Binding name used within query. */
	public abstract void setPovName(String value);

}
