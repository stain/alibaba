package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/04/metadata#Parameter")
public interface Parameter {

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#name")
	String getMetaName();
	void setMetaName(String name);

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#namespace")
	Object getMetaNamespace();
	void setMetaNamespace(Object namespace);

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#range")
	Object getMetaRange();
	void setMetaRange(Object range);

}
