package org.openrdf.server.metadata.resources;

import java.io.File;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class PostResource extends MetadataResource {

	public PostResource(File file, RDFObject target) {
		super(file, target);
	}

	public Response post(Request req) throws Throwable {
		return invokeMethod(req);
	}

}
