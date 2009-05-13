package org.openrdf.server.metadata.behaviours;

import java.io.File;

import org.openrdf.server.metadata.concepts.WebResource;

public abstract class NoMetadataSupport implements WebResource {

	public void extractMetadata(File file) {
		// no metadata
	}
}
