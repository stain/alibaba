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
package org.openrdf.server.metadata.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Reads RDFObjects from an HTTP message body.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectReader implements MessageBodyReader<Object> {
	private GraphMessageReader delegate = new GraphMessageReader();
	private StringBodyReader reader = new StringBodyReader();

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		if (mediaType != null && mediaType.startsWith("text/plain")) {
			if (!reader.isReadable(String.class, String.class, mediaType, con))
				return false;
		} else {
			Class<GraphQueryResult> t = GraphQueryResult.class;
			if (mediaType != null && !delegate.isReadable(t, t, mediaType, con))
				return false;
		}
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return true;
		if (RDFObject.class.isAssignableFrom(type))
			return true;
		return con.getObjectFactory().isNamedConcept(type);
	}

	public Object readFrom(Class<?> type, Type genericType, String media,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws QueryResultParseException,
			TupleQueryResultHandlerException, IOException,
			QueryEvaluationException, RepositoryException {
		Resource subj = null;
		if (location != null) {
			ValueFactory vf = con.getValueFactory();
			subj = vf.createURI(location);
		}
		if (media != null && media.startsWith("text/plain")) {
			ValueFactory vf = con.getValueFactory();
			String uri = reader.readFrom(String.class, String.class, media, in,
					charset, base, location, con);
			subj = vf.createURI(uri);
		} else if (media != null) {
			Class<GraphQueryResult> t = GraphQueryResult.class;
			GraphQueryResult result = delegate.readFrom(t, t, media, in,
					charset, base, location, con);
			try {
				while (result.hasNext()) {
					Statement st = result.next();
					if (subj == null) {
						subj = st.getSubject();
					}
					con.add(st);
				}
			} finally {
				result.close();
			}
		}
		return con.getObject(subj);
	}

}
