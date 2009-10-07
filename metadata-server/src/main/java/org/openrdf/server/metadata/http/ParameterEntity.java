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
package org.openrdf.server.metadata.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.readers.MessageBodyReader;
import org.xml.sax.SAXException;

public class ParameterEntity implements Entity {
	private MessageBodyReader reader;
	private String[] values;
	private String base;
	private ObjectConnection con;
	private String mimeType;

	public ParameterEntity(MessageBodyReader reader, String mimeType,
			String[] values, String base, ObjectConnection con) {
		this.reader = reader;
		this.mimeType = mimeType;
		this.values = values;
		this.base = base;
		this.con = con;
	}

	public boolean isReadable(Class<?> type, Type genericType) {
		Class<?> componentType = type.getComponentType();
		Class<?> parameterType = getParameterClass(genericType);
		if (String.class.equals(type))
			return true;
		if (type.isArray() && String.class.equals(componentType))
			return true;
		if (Set.class.equals(type) && String.class.equals(parameterType))
			return true;
		if (type.isArray() && isReadable(componentType))
			return true;
		if (Set.class.equals(type) && isReadable(parameterType))
			return true;
		if (reader.isReadable(type, genericType, mimeType, con))
			return true;
		return false;
	}

	public <T> T read(Class<T> type, Type genericType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		Class<?> componentType = type.getComponentType();
		Class<?> parameterType = getParameterClass(genericType);
		if (String.class.equals(type))
			return values.length > 0 ? type.cast(values[0]) : null;
		if (type.isArray() && String.class.equals(componentType))
			return type.cast(values);
		if (Set.class.equals(type) && String.class.equals(parameterType))
			return type.cast(new HashSet<String>(Arrays.asList(values)));
		if (type.isArray() && isReadable(componentType))
			return type.cast(readArray(componentType));
		if (Set.class.equals(type) && isReadable(parameterType))
			return type.cast(readSet(parameterType));
		if (values.length > 0)
			return read(values[0], type, genericType);
		return null;
	}

	private <T> T[] readArray(Class<T> componentType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		T[] result = (T[]) Array.newInstance(componentType, values.length);
		for (int i = 0; i < values.length; i++) {
			result[i] = read(values[i], componentType, componentType);
		}
		return result;
	}

	private <T> Set<T> readSet(Class<T> componentType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		Set<T> result = new LinkedHashSet<T>(values.length);
		for (int i = 0; i < values.length; i++) {
			result.add(read(values[i], componentType, componentType));
		}
		return result;
	}

	private <T> T read(String value, Class<T> type, Type genericType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		Charset charset = Charset.forName("UTF-16");
		byte[] buf = value.getBytes(charset);
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		return (T) (reader.readFrom(type, genericType, mimeType, in, charset,
				base, null, con));
	}

	private boolean isReadable(Class<?> componentType) {
		return reader.isReadable(componentType, componentType, mimeType, con);
	}

	private Class<?> getParameterClass(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			Type[] args = ptype.getActualTypeArguments();
			if (args.length == 1) {
				if (args[0] instanceof Class) {
					return (Class<?>) args[0];
				}
			}
		}
		return null;
	}

}
