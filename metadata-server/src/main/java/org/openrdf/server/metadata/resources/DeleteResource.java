package org.openrdf.server.metadata.resources;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class DeleteResource extends MetadataResource {

	public DeleteResource(File file, WebResource target) {
		super(file, target);
	}

	public Response delete(Request req) throws Throwable {
		String name = req.getOperation();
		if (name == null) {
			File file = getFile();
			if (!file.exists())
				return new Response().notFound();
			if (!file.delete())
				return methodNotAllowed(req);
			getWebResource().setMediaType(null);
			getWebResource().setRedirect(null);
			getObjectConnection().setAutoCommit(true);
			return new Response().noContent();
		
		} else {
			// lookup method
			Method method = findSetterMethod(name);
			if (method == null)
				return methodNotAllowed(req);
			try {
				// invoke method
				invoke(method, req);
				// save any changes made
				getObjectConnection().setAutoCommit(true);
				return new Response().noContent();
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}

}
