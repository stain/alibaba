package org.openrdf.server.metadata.concepts;

import java.io.File;
import java.io.IOException;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;

@rdf("http://www.openrdf.org/rdf/2009/metadata#WebResource")
public interface WebResource extends RDFObject {
	@rdf("http://www.openrdf.org/rdf/2009/metadata#redirect")
	WebResource getRedirect();

	void setRedirect(WebResource redirect);

	@rdf("http://www.openrdf.org/rdf/2009/metadata#mediaType")
	String getMediaType();

	void setMediaType(String mediaType);

	@rdf("http://www.openrdf.org/rdf/2009/metadata#extractMetadata")
	void extractMetadata(File file) throws RepositoryException, IOException;

}
