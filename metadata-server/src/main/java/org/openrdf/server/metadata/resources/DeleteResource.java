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

import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

/**
 * Handles DELETE methods.
 * 
 * @author James Leigh
 *
 */
public class DeleteResource extends MetadataResource {

	public DeleteResource(File file, RDFResource target) {
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
		if (!file.getParentFile().canWrite())
			return methodNotAllowed(req);
		ObjectConnection con = getObjectConnection();
		target.setRedirect(null);
		target.setRevision(null);
		if (target instanceof WebResource) {
			removeMediaType();
			con.clear(getURI());
		}
		con.setAutoCommit(true); // prepare()
		if (!file.delete())
			return methodNotAllowed(req);
		return new Response().noContent();
	}

}
