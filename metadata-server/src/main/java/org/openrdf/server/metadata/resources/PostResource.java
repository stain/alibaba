package org.openrdf.server.metadata.resources;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class PostResource extends MetadataResource {

	public PostResource(File file, RDFObject target) {
		super(file, target);
	}

	public Response post(Request req) throws Throwable {
		String name = req.getOperation();
		// lookup method
		List<Method> methods = findOperationMethods(name);
		if (methods.isEmpty())
			return methodNotAllowed(req);
		Method method = findBestMethod(req, methods);
		if (method == null)
			return new Response().badRequest();
		try {
			Object entity = invoke(method, req);
			// save any changes made
			getObjectConnection().setAutoCommit(true);
			// return result
			if (entity instanceof RDFObject && !getTarget().equals(entity)) {
				Resource resource = ((RDFObject) entity).getResource();
				if (resource instanceof URI) {
					URI uri = (URI) resource;
					return new Response().status(307).location(uri.stringValue());
				}
			}
			if (entity == null) {
				return new Response().noContent();
			}
			return new Response().entity(entity);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
