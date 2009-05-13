package org.openrdf.server.metadata.resources;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

import eu.medsea.util.MimeUtil;

public class GetResource extends MetadataResource {

	public GetResource(File file, RDFObject target) {
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

	private Response getData(Request req) throws ParseException,
			RepositoryException {
		String operation;
		File file = getFile();
		WebResource target = getWebResource();
		if (target != null && target.getRedirect() != null) {
			String obj = target.getRedirect().getResource().stringValue();
			return new Response().status(307).location(obj);
		} else if (file.canRead() && req.isAcceptable(getContentType())) {
			Response rb;
			rb = new Response();
			rb = rb.type(getContentType());
			rb = rb.entity(file);
			return rb;
		} else if ((operation = getOperationName("alternate", req)) != null) {
			String loc = getURI().stringValue() + "?" + operation;
			return new Response().status(302).location(loc);
		} else if ((operation = getOperationName("describedby", req)) != null) {
			String loc = getURI().stringValue() + "?" + operation;
			return new Response().status(303).location(loc);
		} else if (file.canRead()) {
			// we could send a 406 Not Acceptable
			Response rb;
			rb = new Response();
			rb = rb.type(getContentType());
			rb = rb.entity(file);
			return rb;
		} else if (file.exists()) {
			return methodNotAllowed(req);
		} else {
			return new Response().notFound();
		}
	}

	private Response getOperationResponse(String name, Request req)
			throws RepositoryException, IOException, IllegalAccessException,
			Throwable {
		// lookup method
		List<Method> methods = findGetterMethods(name);
		if (methods.isEmpty())
			return methodNotAllowed(req);
		Method method = findBestMethod(req, methods);
		if (method == null)
			return new Response().badRequest();
		try {
			// invoke method
			Object entity = invoke(method, req);
			// return result
			if (entity instanceof RDFObject && !getTarget().equals(entity)) {
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

	private String getContentType() throws RepositoryException {
		WebResource target = getWebResource();
		if (target != null && target.getMediaType() != null)
			return target.getMediaType();
		target = addWebResourceDesignation();
		String mimeType = MimeUtil.getMagicMimeType(getFile());
		if (mimeType == null)
			return "application/octet-stream";
		target.setMediaType(mimeType);
		getObjectConnection().commit();
		return mimeType;
	}

}
