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
package org.openrdf.server.metadata.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

/**
 * Handles PUT requests.
 * 
 * @author James Leigh
 * 
 */
public class PutController extends Controller {

	public PutController(File file, RDFResource target) {
		super(file, target);
	}

	public Response put(Request req) throws Throwable {
		Response resp = invokeMethod(req, false);
		if (resp != null) {
			return resp;
		}
		ObjectConnection con = getObjectConnection();
		String loc = req.getHeader("Content-Location");
		Response rb = new Response().noContent();
		if (req.getContentType() == null && loc != null) {
			ObjectFactory of = con.getObjectFactory();
			URI uri = createURI(loc);
			RDFResource redirect = of.createObject(uri, RDFResource.class);
			target.setRedirect(redirect);
			con.setAutoCommit(true); // flush()
			rb.eTag(con.getObject(WebResource.class, target.getResource()));
			return rb;
		}
		try {
			File file = getFile();
			File dir = file.getParentFile();
			dir.mkdirs();
			if (!dir.canWrite())
				return methodNotAllowed(req);
			File tmp = new File(dir, "$partof" + file.getName());
			InputStream in = req.getInputStream();
			OutputStream out = new FileOutputStream(tmp);
			try {
				byte[] buf = new byte[512];
				int read;
				while ((read = in.read(buf)) >= 0) {
					out.write(buf, 0, read);
				}
			} finally {
				out.close();
			}
			String contentType = req.getContentType();
			if (contentType != null) {
				target.setRedirect(null);
				WebResource web = setMediaType(contentType);
				target = web;
				URI uri = getURI();
				con.clear(uri);
				con.setAddContexts(uri);
				web.extractMetadata(tmp);
				con.setAutoCommit(true); // flush()
				rb.eTag(con.getObject(WebResource.class, target.getResource()));
			}
			con.setAutoCommit(true); // prepare()
			if (file.exists()) {
				file.delete();
			}
			if (!tmp.renameTo(file)) {
				tmp.delete();
				return methodNotAllowed(req);
			}
			rb.lastModified(file.lastModified());
			return rb;
		} catch (FileNotFoundException e) {
			return methodNotAllowed(req);
		}
	}

}
