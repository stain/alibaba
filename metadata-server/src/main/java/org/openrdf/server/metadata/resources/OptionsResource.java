package org.openrdf.server.metadata.resources;

import java.io.File;

import org.openrdf.repository.RepositoryException;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class OptionsResource extends MetadataResource {

	public OptionsResource(File file, WebResource target) {
		super(file, target);
	}

	public Response options(Request req) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response().header("Allow", sb.toString());
	}

}
