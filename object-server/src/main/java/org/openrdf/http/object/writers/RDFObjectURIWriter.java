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
package org.openrdf.http.object.writers;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import org.openrdf.http.object.writers.base.URIListWriter;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.QueryResult;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF URI from RDFObject(s).
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectURIWriter extends URIListWriter<Object> {

	public RDFObjectURIWriter() {
		super(Object.class);
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		if (!super.isWriteable(mimeType, type, type, of))
			return false;
		if (QueryResult.class.isAssignableFrom(type))
			return false;
		if (of == null)
			return false;
		Class<?> c = getComponestType(type, genericType);
		if (Object.class.equals(c) || RDFObject.class.equals(c))
			return true;
		return of.isNamedConcept(c);
	}

	@Override
	protected String toString(Object result) {
		Resource resource = ((RDFObject) result).getResource();
		if (resource instanceof URI)
			return resource.stringValue();
		return "_:" + resource.stringValue();
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
