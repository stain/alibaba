/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Reads RDF Object datatypes from an HTTP message body.
 * 
 * @author James Leigh
 * 
 */
public class DatatypeReader implements MessageBodyReader<Object> {
	private StringBodyReader delegate = new StringBodyReader();

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return false;
		if (!delegate.isReadable(String.class, String.class, mediaType, con))
			return false;
		return con.getObjectFactory().isDatatype(type);
	}

	public Object readFrom(Class<?> type, Type genericType, String media,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws QueryResultParseException,
			TupleQueryResultHandlerException, IOException,
			QueryEvaluationException, RepositoryException {
		String value = delegate.readFrom(String.class, String.class, media, in,
				charset, base, location, con);
		ValueFactory vf = con.getValueFactory();
		ObjectFactory of = con.getObjectFactory();
		URI datatype = vf.createURI("java:", type.getName());
		Literal lit = vf.createLiteral(value, datatype);
		return type.cast(of.createObject(lit));
	}

}