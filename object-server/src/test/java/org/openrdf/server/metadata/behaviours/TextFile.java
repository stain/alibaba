package org.openrdf.server.metadata.behaviours;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.HTTPFileObject;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;


public abstract class TextFile implements HTTPFileObject {
	@method("GET")
	@type("text/plain")
	public InputStream getInputStream() throws IOException {
		return openInputStream();
	}

	@operation({})
	@method("DELETE")
	public void deleteObject() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		con.clear(getResource());
		con.removeDesignations(this, vf.createURI("urn:mimetype:text/plain"));
		if (!delete())
			throw new MethodNotAllowed();
	}
}
