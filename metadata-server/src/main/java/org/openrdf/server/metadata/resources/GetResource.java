/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.server.metadata.resources;

import java.io.File;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

import eu.medsea.util.MimeUtil;

/**
 * Handles the GET method requests.
 * 
 * @author James Leigh
 *
 */
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
