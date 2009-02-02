package org.openrdf.repository.object.traits;

import java.util.List;

import org.openrdf.query.BindingSet;

public interface PropertyConsumer {
	public static final String USE = "usePropertyBindings";

	void usePropertyBindings(String binding, List<BindingSet> results);
}