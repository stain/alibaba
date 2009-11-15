package org.openrdf.server.metadata.concepts;

import java.io.File;
import java.lang.reflect.Method;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.WebObject;

public interface InternalWebObject extends WebObject {

	void initFileObject(File file, boolean readOnly);

	@iri("http://www.openrdf.org/rdf/2009/metadata#redirect")
	WebObject getRedirect();

	void setRedirect(WebObject redirect);

	@iri("http://www.openrdf.org/rdf/2009/auditing#revision")
	Transaction getRevision();

	void setRevision(Transaction revision);

	void contentChanged();

	String getAndSetMediaType();

	Object invokeRemote(Method method, Object[] parameters) throws Exception;
}
