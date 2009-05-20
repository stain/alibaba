package org.openrdf.server.metadata.resources;

import java.io.File;

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
		String operation;
		File file = getFile();
		WebResource target = getWebResource();
		Response resp = invokeMethod(req, true);
		if (resp != null) {
			if (resp.isNoContent())
				return new Response().notFound("Not Found "
						+ req.getRequestURL());
			return resp;
		} else if (target != null && target.getRedirect() != null) {
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
			return new Response().notFound("Not Found " + req.getRequestURL());
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
		setMediaType(mimeType);
		getObjectConnection().commit();
		return mimeType;
	}

}
