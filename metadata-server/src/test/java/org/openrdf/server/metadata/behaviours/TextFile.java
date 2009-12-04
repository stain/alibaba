package org.openrdf.server.metadata.behaviours;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.server.metadata.WebObject;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.type;


public abstract class TextFile implements WebObject {
	@method("GET")
	@type("text/plain")
	public InputStream getInputStream() throws IOException {
		return openInputStream();
	}
}
