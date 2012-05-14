package org.openrdf.repository.object.advice;

import org.openrdf.repository.object.traits.ObjectMessage;

/**
 * Implemented by advisers that handler or intercept method invocations based on
 * method annotations.
 * 
 * @author James Leigh
 * 
 */
public interface Advice {

	public Object intercept(ObjectMessage message) throws Throwable;
}
