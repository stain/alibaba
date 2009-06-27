package org.openrdf.server.metadata.concepts;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;

@rdf(RDFS.NAMESPACE + "Resource")
public interface RDFResource extends RDFObject {

	String variantTag(String mediaType);

	long lastModified();

	@rdf("http://www.openrdf.org/rdf/2009/auditing#revision")
	Transaction getRevision();

	void setRevision(Transaction revision);

	@rdf("http://www.openrdf.org/rdf/2009/metadata#redirect")
	RDFResource getRedirect();

	void setRedirect(RDFResource redirect);
}
