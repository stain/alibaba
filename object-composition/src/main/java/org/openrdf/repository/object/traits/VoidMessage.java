package org.openrdf.repository.object.traits;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.vocabulary.OBJ;

public interface VoidMessage {

	/** The receiver of this message. */
	@iri(MSG.NAMESPACE + "target")
	Object getMsgTarget();

	/** The parameter values used in this message. */
	Object[] getParameters();

	/** The parameter values used in this message. */
	void setParameters(Object[] objParameters);

	/** Called to allow the message to proceed to the next implementation method. */
	@iri(OBJ.NAMESPACE + "proceed")
	void proceed();

}
