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
package org.openrdf.server.metadata.writers;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.openrdf.model.Resource;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;

/**
 * Writes RDF DESCRIBE from an RDFObject.
 * 
 * @author James Leigh
 *
 */
public class RDFObjectWriter implements MessageBodyWriter<RDFObject> {
	private static final String DESCRIBE_SELF = "CONSTRUCT {$self ?pred ?obj}\n"
			+ "WHERE {$self ?pred ?obj}";
	private GraphMessageWriter delegate;

	public RDFObjectWriter() {
		delegate = new GraphMessageWriter();
	}

	public long getSize(RDFObject t, String mimeType) {
		return -1;
	}

	public boolean isWriteable(Class<?> type, String mimeType) {
		Class<GraphQueryResult> t = GraphQueryResult.class;
		if (!delegate.isWriteable(t, mimeType))
			return false;
		return RDFObject.class.isAssignableFrom(type);
	}

	public String getContentType(Class<?> type, String mimeType, Charset charset) {
		return delegate.getContentType(null, mimeType, null);
	}

	public void writeTo(RDFObject result, String base, String mimeType,
			OutputStream out, Charset charset) throws IOException, RDFHandlerException,
			QueryEvaluationException, TupleQueryResultHandlerException,
			RepositoryException {
		ObjectConnection con = result.getObjectConnection();
		Resource resource = result.getResource();
		try {
			GraphQuery query = con.prepareGraphQuery(SPARQL, DESCRIBE_SELF);
			query.setBinding("self", resource);
			delegate.writeTo(query.evaluate(), base, mimeType, out, null);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

}
