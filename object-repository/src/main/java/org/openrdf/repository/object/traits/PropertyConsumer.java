package org.openrdf.repository.object.traits;

import java.util.Collection;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

public interface PropertyConsumer {
	public static final String USE = "usePropertyValues";

	void usePropertyValues(Map<URI, ? extends Collection<Value>> results);
}