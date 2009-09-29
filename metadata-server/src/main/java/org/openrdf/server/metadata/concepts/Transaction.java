package org.openrdf.server.metadata.concepts;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.iri;

@iri("http://www.openrdf.org/rdf/2009/auditing#Transaction")
public interface Transaction extends RDFObject {

	@iri("http://www.openrdf.org/rdf/2009/auditing#committedOn")
	XMLGregorianCalendar getCommittedOn();

	void setCommittedOn(XMLGregorianCalendar committedOn);
}
