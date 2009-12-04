package org.openrdf.server.metadata.behaviours;

import java.io.IOException;
import java.net.URL;

import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.concepts.Alias;

public abstract class AliasSupport implements Alias {
	@operation({})
	@method("GET")
	public URL getAlias() throws IOException {
		return getRedirectsTo().toUri().toURL();
	}

}
