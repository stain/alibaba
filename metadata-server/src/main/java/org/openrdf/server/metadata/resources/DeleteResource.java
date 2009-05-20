package org.openrdf.server.metadata.resources;

import java.io.File;

import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class DeleteResource extends MetadataResource {

	public DeleteResource(File file, RDFObject target) {
		super(file, target);
	}

	public Response delete(Request req) throws Throwable {
		Response resp = invokeMethod(req, false);
		if (resp != null) {
			return resp;
		}
		File file = getFile();
		if (!file.exists())
			return new Response().notFound();
		if (!file.delete())
			return methodNotAllowed(req);
		WebResource target = getWebResource();
		if (target != null) {
			target.setRedirect(null);
			target = setMediaType(null);
			ObjectConnection con = getObjectConnection();
			con.removeDesignation(target, WebResource.class);
			con.clear(getURI());
			con.setAutoCommit(true);
		}
		return new Response().noContent();
	}

}
