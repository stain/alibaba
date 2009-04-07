package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/04/metadata#Query")
public interface Query {

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#inSparql")
	String getInSparql();
	void setinSparql(String sparql);
}
