package org.openrdf.repository.object.concepts;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/object#Message")
public interface Message {

	/** The receiver of this message. */
	@rdf("http://www.openrdf.org/rdf/2009/object#target")
	Object getTarget();
	/** The receiver of this message. */
	@rdf("http://www.openrdf.org/rdf/2009/object#target")
	void setTarget(Object objTarget);

	/** The parameter values used in this message. */
	@rdf("http://www.openrdf.org/rdf/2009/object#parameters")
	Object[] getParameters();
	/** The parameter values used in this message. */
	@rdf("http://www.openrdf.org/rdf/2009/object#parameters")
	void setParameters(Object[] objParameters);

	/** Called to allow the message to proceed to the next implementation method. */
	@rdf("http://www.openrdf.org/rdf/2009/object#proceed")
	Object proceed();

}
