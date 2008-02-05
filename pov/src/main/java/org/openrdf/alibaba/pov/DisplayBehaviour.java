package org.openrdf.alibaba.pov;

import java.util.Collection;

import org.openrdf.alibaba.exceptions.AlibabaException;

/** Methods to extract values from a resource. */
public interface DisplayBehaviour {
	public abstract Collection<?> getValuesOf(Object resource)
			throws AlibabaException;

	public abstract void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException;
}
