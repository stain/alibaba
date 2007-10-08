package org.openrdf.alibaba.decor;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.openrdf.alibaba.exceptions.AlibabaException;

public interface DecorationBehaviour {
	public abstract boolean isSeparation();

	public abstract boolean isBefore(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract boolean isAfter(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract boolean isSeparation(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract void before(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract void after(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract void separation(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract void empty(Map<String, ?> bindings)
			throws AlibabaException, IOException;

	public abstract void values(Collection values, Map<String, ?> bindings)
			throws AlibabaException, IOException;
}
