package org.openrdf.server.metadata.concepts;

import java.io.IOException;

import javax.tools.FileObject;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.matches;

@matches("file:*")
public interface LocalFileObject extends RDFObject, FileObject {

	void commitFile() throws IOException;

	void rollbackFile();
}
