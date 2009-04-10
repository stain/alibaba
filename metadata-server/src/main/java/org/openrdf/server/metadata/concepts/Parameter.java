package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/04/metadata#Parameter")
public interface Parameter {

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#name")
	String getMetaName();
	void setMetaName(String name);

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#base")
	Object getMetaBase();
	void setMetaBase(Object base);

	@rdf("http://www.openrdf.org/rdf/2009/04/metadata#datatype")
	Object getMetaDatatype();
	void setMetaDatatype(Object datatype);

}
