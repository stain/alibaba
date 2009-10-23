package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.WebObject;

@iri("http://www.openrdf.org/rdf/2009/metadata#WebRedirect")
public interface WebRedirect extends WebObject {

	@iri("http://www.openrdf.org/rdf/2009/metadata#redirect")
	WebObject getRedirect();

	void setRedirect(WebObject redirect);

}
