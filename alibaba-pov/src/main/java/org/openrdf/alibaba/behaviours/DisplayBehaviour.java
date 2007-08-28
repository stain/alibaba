package org.openrdf.alibaba.behaviours;

import java.util.Set;

import org.openrdf.alibaba.exceptions.AlibabaException;

/** Description of how this resource should be shown. */
public interface DisplayBehaviour {
	/**
	 * Set of MessageFormat parameters.
	 * @param resource
	 * @return Set of MessageFormat parameters.
	 * @throws AlibabaException
	 */
	public abstract Set<?> getValuesOf(Object resource)
			throws AlibabaException;

	public abstract void setValuesOf(Object resource, Set<?> values)
			throws AlibabaException;
}
