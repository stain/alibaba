package org.openrdf.http.object.concepts;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.iri;

public interface VersionedObject extends RDFObject {

	@iri("http://www.openrdf.org/rdf/2009/auditing#revision")
	Transaction getRevision();

	@iri("http://www.openrdf.org/rdf/2009/auditing#revision")
	void setRevision(Transaction revision);

	void touchRevision();

}
