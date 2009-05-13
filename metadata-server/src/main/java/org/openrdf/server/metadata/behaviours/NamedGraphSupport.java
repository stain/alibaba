package org.openrdf.server.metadata.behaviours;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.server.metadata.concepts.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NamedGraphSupport implements WebResource {
	private Logger logger = LoggerFactory.getLogger(NamedGraphSupport.class);

	public void extractMetadata(File file) throws RepositoryException, IOException {
		ObjectConnection con = getObjectConnection();
		String media = getMediaType();
		MediaType m = MediaType.valueOf(media);
		String mime = m.getType() + "/" + m.getSubtype();
		RDFFormat format = RDFFormat.forMIMEType(mime);
		try {
			con.add(file, getResource().stringValue(), format);
		} catch (RDFParseException e) {
			logger.warn(e.getMessage(), e);
		}
	}
}
