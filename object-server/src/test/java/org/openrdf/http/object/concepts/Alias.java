package org.openrdf.http.object.concepts;

import org.openrdf.repository.object.annotations.iri;

@iri("http://www.openrdf.org/rdf/2009/auditing#Alias")
public interface Alias {

	@iri("http://www.openrdf.org/rdf/2009/httpobject#redirectsTo")
	HTTPFileObject getRedirectsTo();

	void setRedirectsTo(HTTPFileObject redirectsTo);

}
