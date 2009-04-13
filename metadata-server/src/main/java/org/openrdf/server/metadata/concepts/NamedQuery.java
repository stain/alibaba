package org.openrdf.server.metadata.concepts;

import java.util.Set;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/meta#NamedQuery")
public interface NamedQuery {

	@rdf("http://www.openrdf.org/rdf/2009/meta#inSparql")
	String getMetaInSparql();
	void setMetaInSparql(String sparql);

	@rdf("http://www.openrdf.org/rdf/2009/meta#parameter")
	Set<Parameter> getMetaParameters();
	void setMetaParameters(Set<Parameter> parameters);
}
