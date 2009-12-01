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
package org.openrdf.server.metadata.readers.base;

import info.aduna.net.ParsedURI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.readers.MessageBodyReader;

public abstract class URIListReader<URI> implements MessageBodyReader<Object> {
	private Class<URI> componentType;

	public URIListReader(Class<URI> componentType) {
		this.componentType = componentType;
	}

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		if (Set.class.equals(type)) {
			if (genericType instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) genericType;
				Type ctype = ptype.getActualTypeArguments()[0];
				if (ctype instanceof Class && !componentType.equals(ctype))
					return false;
			}
		} else if (!componentType.equals(type)) {
			return false;
		}
		return mediaType != null && mediaType.startsWith("text/");
	}

	public Object readFrom(Class<?> type, Type genericType, String media,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws QueryResultParseException,
			TupleQueryResultHandlerException, IOException,
			QueryEvaluationException, RepositoryException {
		if (location != null) {
			URI url;
			if (base == null) {
				url = create(con, location);
			} else {
				ParsedURI uri = new ParsedURI(base);
				uri.normalize();
				ParsedURI result = new ParsedURI(location);
				url = create(con, uri.resolve(result).toString());
			}
			if (Set.class.equals(type)) {
				Set<URI> set = new HashSet<URI>();
				set.add(url);
				return set;
			} else {
				return url;
			}
		}
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in,
				charset));
		try {
			Set<URI> set = Set.class.equals(type) ? new HashSet<URI>() : null;
			String str;
			while ((str = reader.readLine()) != null) {
				if (str.startsWith("#") || str.isEmpty())
					continue;
				URI url;
				if (base != null) {
					ParsedURI uri = new ParsedURI(base);
					uri.normalize();
					ParsedURI result = new ParsedURI(str);
					url = create(con, uri.resolve(result).toString());
				} else {
					url = create(con, str);
				}
				if (set == null)
					return url;
				set.add(url);
			}
			return set;
		} finally {
			reader.close();
		}
	}

	protected abstract URI create(ObjectConnection con, String uri)
			throws MalformedURLException;

}
