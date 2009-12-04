package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.WebObject;

@iri("http://www.openrdf.org/rdf/2009/auditing#Alias")
public interface Alias {

	@iri("http://www.openrdf.org/rdf/2009/metadata#redirectsTo")
	WebObject getRedirectsTo();

	void setRedirectsTo(WebObject redirectsTo);

}
