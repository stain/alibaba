package org.openrdf.http.object.behaviours;

import java.io.IOException;
import java.net.URL;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.concepts.Alias;

public abstract class AliasSupport implements Alias {
	@operation({})
	@method("GET")
	public URL getAlias() throws IOException {
		return getRedirectsTo().toUri().toURL();
	}

}
