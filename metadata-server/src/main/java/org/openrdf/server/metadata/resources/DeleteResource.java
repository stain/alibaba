package org.openrdf.server.metadata.resources;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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
		String name = req.getOperation();
		ObjectConnection con = getObjectConnection();
		if (name == null) {
			File file = getFile();
			if (!file.exists())
				return new Response().notFound();
			if (!file.delete())
				return methodNotAllowed(req);
			WebResource target = getWebResource();
			if (target != null) {
				target.setMediaType(null);
				target.setRedirect(null);
				con.removeDesignation(target, WebResource.class);
				con.setAutoCommit(true);
			}
			return new Response().noContent();
		
		} else {
			// lookup method
			List<Method> methods = findSetterMethods(name);
			if (methods.isEmpty())
				return methodNotAllowed(req);
			Method method = findBestMethod(req, methods);
			if (method == null)
				return new Response().badRequest();
			try {
				// invoke method
				invoke(method, req);
				// save any changes made
				con.setAutoCommit(true);
				return new Response().noContent();
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}

}
