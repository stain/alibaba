package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.iri;

@iri("http://www.openrdf.org/rdf/2009/auditing#Alias")
public interface Alias {

	@iri("http://www.openrdf.org/rdf/2009/metadata#redirectsTo")
	HTTPFileObject getRedirectsTo();

	void setRedirectsTo(HTTPFileObject redirectsTo);

}
