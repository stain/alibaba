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
package org.openrdf.http.object.readers;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.Set;

import org.openrdf.http.object.readers.base.URIListReader;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Reads RDFObjects from a URI list.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectURIReader extends URIListReader<Object> {

	public RDFObjectURIReader() {
		super(Object.class);
	}

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		if (!super.isReadable(type, type, mediaType, con))
			return false;
		Class<?> c = getComponestType(type, genericType);
		if (Object.class.equals(c))
			return true;
		if (RDFObject.class.isAssignableFrom(c))
			return true;
		return con.getObjectFactory().isNamedConcept(c);
	}

	@Override
	protected Object create(ObjectConnection con, String uri)
			throws MalformedURLException, RepositoryException {
		if (uri != null && uri.startsWith("_:"))
			return con.getObject(con.getValueFactory().createBNode(uri.substring(2)));
		return con.getObject(uri);
	}

	private Class<?> getComponestType(Class<?> type, Type genericType) {
		if (Set.class.equals(type)) {
			if (genericType instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) genericType;
				Type ctype = ptype.getActualTypeArguments()[0];
				if (ctype instanceof Class) {
					return (Class) ctype;
				}
			}
			return Object.class;
		} else {
			return type;
		}
	}

}
