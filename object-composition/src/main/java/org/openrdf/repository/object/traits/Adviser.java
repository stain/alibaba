package org.openrdf.repository.object.traits;

/**
 * Implemented by advisers that handler or intercept method invocations based on
 * method annotations.
 * 
 * @author James Leigh
 * 
 */
public interface Adviser {

	public Object intercept(ObjectMessage message) throws Throwable;
}
