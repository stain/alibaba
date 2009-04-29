package org.openrdf.server.metadata.resources;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class GetResource extends MetadataResource {

	public GetResource(File file, WebResource target) {
		super(file, target);
	}

	public Response get(Request req) throws Throwable {
		String name = req.getOperation();
		if (name == null) {
			return getData(req);
		} else {
			return getOperationResponse(name, req);
		}
	}

	private Response getData(Request req)
			throws RepositoryException {
		File file = getFile();
		if (file.canRead()) {
			Response rb;
			rb = new Response();
			rb = rb.type(getContentType());
			rb = rb.entity(file);
			return rb;
		} else if (file.exists()) {
			return methodNotAllowed(req);
		} else if (getWebResource().getRedirect() != null) {
			String obj = getWebResource().getRedirect().getResource()
					.stringValue();
			return new Response().status(307).location(obj);
		} else {
			String alt = getOperationName("alternate");
			if (alt != null) {
				String loc = getURI().stringValue() + "?" + alt;
				return new Response().status(302).location(loc);
			}
			String describe = getOperationName("describedby");
			if (describe != null) {
				String loc = getURI().stringValue() + "?" + describe;
				return new Response().status(302).location(loc);
			}
			return new Response().notFound();
		}
	}

	private Response getOperationResponse(String name, Request req)
			throws RepositoryException, IOException, IllegalAccessException,
			Throwable {
		// lookup method
		Method method = findGetterMethod(name);
		if (method == null)
			return methodNotAllowed(req);
		try {
			// invoke method
			Object entity = invoke(method, req);
			// return result
			if (entity instanceof RDFObject && !getWebResource().equals(entity)) {
				Resource resource = ((RDFObject) entity).getResource();
				if (resource instanceof URI) {
					URI uri = (URI) resource;
					return new Response().status(307).location(
							uri.stringValue());
				}
			}
			if (entity == null) {
				return new Response().notFound("Not Found <"
						+ getURI().stringValue() + "?" + name + ">");
			}
			return new Response().entity(entity);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
