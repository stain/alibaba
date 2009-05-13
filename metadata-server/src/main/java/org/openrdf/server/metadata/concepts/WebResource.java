package org.openrdf.server.metadata.concepts;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/meta#WebResource")
public interface WebResource extends RDFObject {
	@rdf("http://www.openrdf.org/rdf/2009/meta#redirect")
	WebResource getRedirect();
	void setRedirect(WebResource redirect);

	@rdf("http://www.openrdf.org/rdf/2009/meta#mediaType")
	String getMediaType();
	void setMediaType(String mediaType);

}
