package org.openrdf.http.object.concepts;

import java.io.IOException;

import javax.tools.FileObject;

import org.openrdf.repository.object.annotations.matches;

@matches("file:*")
public interface LocalFileObject extends VersionedObject, FileObject {

	void commitFile() throws IOException;

	void rollbackFile();
}
