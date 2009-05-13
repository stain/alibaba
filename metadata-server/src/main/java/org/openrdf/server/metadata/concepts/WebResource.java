package org.openrdf.server.metadata.concepts;

import java.io.File;
import java.io.IOException;

import org.openrdf.repository.RepositoryException;
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

	@rdf("http://www.openrdf.org/rdf/2009/meta#extractMetadata")
	void extractMetadata(File file) throws RepositoryException, IOException;

}
