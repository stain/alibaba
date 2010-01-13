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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.http.object.util.GenericType;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Readers a percent encoded form into a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public final class FormMapMessageReader implements
		MessageBodyReader<Map<String, Object>> {
	private MessageBodyReader delegate = AggregateReader.getInstance();

	public boolean isReadable(Class<?> ctype, Type gtype, String mimeType,
			ObjectConnection con) {
		GenericType<?> type = new GenericType(ctype, gtype);
		if (!type.isMap() || !type.isKeyUnknownOr(String.class))
			return false;
		GenericType<?> vt = type.getComponentGenericType();
		if (vt.isSetOrArray()) {
			Class<?> vvc = vt.getComponentClass();
			Type vvt = vt.getComponentType();
			if (!delegate.isReadable(vvc, vvt, "text/plain", con))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isReadable(vt.clas(), vt.type(), "text/plain", con))
				return false;
		}
		return mimeType != null
				&& mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public Map<String, Object> readFrom(Class<?> ctype, Type gtype,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con) throws IOException,
			QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		GenericType<Map> type = new GenericType(ctype, gtype);
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		GenericType<?> vtype = type.getComponentGenericType();
		if (vtype.isUnknown()) {
			Class<?> sc = String[].class;
			vtype = new GenericType(sc, sc);
			type = new GenericType(Map.class, new ParameterizedType() {
				public Type getRawType() {
					return null;
				}

				public Type getOwnerType() {
					return null;
				}

				public Type[] getActualTypeArguments() {
					return new Type[] { String.class, String[].class };
				}
			});
		}
		try {
			Map parameters = new LinkedHashMap();
			Scanner scanner = new Scanner(in, charset.name());
			scanner.useDelimiter("&");
			while (scanner.hasNext()) {
				String[] nameValue = scanner.next().split("=", 2);
				if (nameValue.length == 0 || nameValue.length > 2)
					continue;
				String name = decode(nameValue[0]);
				if (nameValue.length < 2) {
					if (!parameters.containsKey(name)) {
						parameters.put(name, new ArrayList());
					}
				} else {
					String value = decode(nameValue[1]);
					Collection values = (Collection) parameters.get(name);
					if (values == null) {
						parameters.put(name, values = new ArrayList());
					}
					ByteArrayInputStream vin = new ByteArrayInputStream(value
							.getBytes(charset));
					if (vtype.isSetOrArray()) {
						Class<?> vc = vtype.getComponentClass();
						Type vt = vtype.getComponentType();
						values.add(delegate.readFrom(vc, vt, "text/plain", vin,
								charset, base, null, con));
					} else {
						Class<?> vc = vtype.clas();
						Type vt = vtype.type();
						values.add(delegate.readFrom(vc, vt, "text/plain", vin,
								charset, base, null, con));
					}
				}
			}
			return type.castMap(parameters);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String decode(String v) throws UnsupportedEncodingException {
		return URLDecoder.decode(v, "UTF-8");
	}
}
