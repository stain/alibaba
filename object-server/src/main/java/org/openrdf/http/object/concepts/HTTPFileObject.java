package org.openrdf.http.object.concepts;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.tools.FileObject;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.matches;

@matches( { "http://*", "https://*" })
public interface HTTPFileObject extends RDFObject, FileObject {

	void initLocalFileObject(File file, boolean readOnly);

	void commitFile() throws IOException;

	void rollbackFile();

	@iri("http://www.openrdf.org/rdf/2009/auditing#revision")
	Transaction getRevision();

	@iri("http://www.openrdf.org/rdf/2009/auditing#revision")
	void setRevision(Transaction revision);

	String revisionTag();

	String variantTag(String mediaType);

	Object invokeRemote(Method method, Object[] parameters) throws Exception;
}
