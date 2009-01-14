package org.openrdf.elmo;

import org.openrdf.elmo.annotations.rdf;
import java.util.Set;

/** The base class for all messages. */
public interface Message extends Entity {
	/** The return value of this message. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#literalResponse")
	Set<Object> getElmoLiteralResponse();
	/** The return value of this message. */
	void setElmoLiteralResponse(Set<?> elmoLiteralResponse);

	/** The return value of this message. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#objectResponse")
	Set<Object> getElmoObjectResponse();
	/** The return value of this message. */
	void setElmoObjectResponse(Set<?> elmoObjectResponse);

	/** The receiver of this message. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#target")
	Object getElmoTarget();
	/** The receiver of this message. */
	void setElmoTarget(Object elmoTarget);

	/** Send this message to its target. */
	@rdf("http://www.openrdf.org/rdf/2008/08/elmo#invoke")
	Set<Object> elmoInvoke();

}
