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
import java.util.Collection;
import java.util.Iterator;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;


/**
 * Handles the GET method requests.
 * 
 * @author James Leigh
 *
 */
public class GetResource extends MetadataResource {

	public GetResource(File file, RDFResource target) {
		super(file, target);
	}

	public Response get(Request req) throws Throwable {
		String operation;
		File file = getFile();
		Response rb = invokeMethod(req, true);
		if (rb != null) {
			if (rb.isNoContent()) {
				rb = new Response().notFound("Not Found "
						+ req.getRequestURL());
			}
		} else if (target.getRedirect() != null) {
			String obj = target.getRedirect().getResource().stringValue();
			rb = new Response().status(307).location(obj);
			rb.eTag(target);
		} else if (file.canRead() && req.isAcceptable(getContentType())) {
			rb = new Response();
			rb = rb.type(getContentType());
			rb = rb.entity(file, target);
		} else if ((operation = getOperationName("alternate", req)) != null) {
			String loc = getURI().stringValue() + "?" + operation;
			rb = new Response().status(302).location(loc);
			rb.eTag(target);
		} else if ((operation = getOperationName("describedby", req)) != null) {
			String loc = getURI().stringValue() + "?" + operation;
			rb = new Response().status(303).location(loc);
			rb.eTag(target);
		} else if (file.canRead()) {
			// we could send a 406 Not Acceptable
			rb = new Response();
			rb = rb.type(getContentType());
			rb = rb.entity(file, target);
		} else if (file.exists()) {
			rb = methodNotAllowed(req);
		} else {
			rb = new Response().notFound("Not Found " + req.getRequestURL());
		}
		if (!rb.getHeaderNames().contains("Cache-Control")) {
			setCacheControl(target.getClass(), rb);
		}
		return rb;
	}

	private String getContentType() throws RepositoryException,
			QueryEvaluationException {
		WebResource target = getWebResource();
		if (target != null && target.getMediaType() != null)
			return target.getMediaType();
		String mimeType = getMimeType(getFile());
		target = setMediaType(mimeType);
		ObjectConnection con = getObjectConnection();
		con.setAutoCommit(true); // flush()
		this.target = con.getObject(WebResource.class, target.getResource());
		return mimeType;
	}

	private String getMimeType(File file) {
		Collection types = MimeUtil.getMimeTypes(file);
		MimeType mimeType = null;
		double specificity = 0;
		for (Iterator it = types.iterator(); it.hasNext();) {
			MimeType mt = (MimeType) it.next();
			int spec = mt.getSpecificity() * 2;
			if (!mt.getSubType().startsWith("x-")) {
				spec += 1;
			}
			if (spec > specificity) {
				mimeType = mt;
				specificity = spec;
			}
		}
		if (mimeType == null)
			return "application/octet-stream";
		return mimeType.toString();
	}

	private void setCacheControl(Class<?> type,
			Response rb) {
		if (type.isAnnotationPresent(cacheControl.class)) {
			for (String value : type.getAnnotation(cacheControl.class).value()) {
				rb.header("Cache-Control", value);
			}
		} else {
			if (type.getSuperclass() != null) {
				setCacheControl(type.getSuperclass(), rb);
			}
			for (Class<?> face : type.getInterfaces()) {
				setCacheControl(face, rb);
			}
		}
	}

}
