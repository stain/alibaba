package org.openrdf.script;

import org.openrdf.model.URI;
import org.openrdf.script.controlers.Insert;

public class ControlRegistry {

	public static ControlRegistry getInstance() {
		return null;
	}

	public Control get(URI uri) {
		return new Insert();
	}

}
