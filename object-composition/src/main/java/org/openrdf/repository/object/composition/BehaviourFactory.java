package org.openrdf.repository.object.composition;

import java.lang.reflect.Method;

/**
 * Behaviour mixin constructor.
 * 
 * @author James Leigh
 * 
 * @param <B>
 *            behaviour class
 */
public interface BehaviourFactory {

	/**
	 * Type of behaviour that the {@link #newInstance(Object)} will implement.
	 * 
	 * @return the type of behaviour
	 */
	Class<?> getBehaviourType();

	/**
	 * Traits that these behaviours provides.
	 * 
	 * @return array of java interfaces
	 */
	Class<?>[] getInterfaces();

	/**
	 * Public methods these behaviours provide. This includes methods from
	 * {@link #getInterfaces()} that these behaviours provides.
	 * 
	 * @return array of public methods
	 */
	Method[] getMethods();

	/**
	 * The method implemented by {@link #getBehaviourType()} that is to be
	 * invoked when the given method is called. If these behaviours do not
	 * provide an implementation for the given method, return null.
	 * 
	 * @param method
	 * @return method implemented by the return value of
	 *         {@link #getBehaviourType()} or null
	 */
	Method getInvocation(Method method);

	/**
	 * If these behaviours should always be invoked before behaviours of the
	 * given factory.
	 * 
	 * @param factory
	 *            an alternative set of behaviours
	 * @return false if no preference
	 */
	boolean precedes(BehaviourFactory factory);

	/**
	 * New behaviour implementation for the given proxy object.
	 * 
	 * @param composed
	 * @return
	 * @throws Throwable
	 */
	Object newInstance(Object proxy) throws Throwable;

}
