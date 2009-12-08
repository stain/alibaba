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
package org.openrdf.http.object.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.http.object.readers.AggregateReader;
import org.openrdf.http.object.readers.MessageBodyReader;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

public class ParameterEntity implements Entity {
	private MessageBodyReader reader = AggregateReader.getInstance();
	private String[] values;
	private String base;
	private ObjectConnection con;
	private String[] mediaTypes;
	private String mimeType;

	public ParameterEntity(String[] mediaTypes, String mimeType, String[] values, String base,
			ObjectConnection con) {
		this.mediaTypes = mediaTypes;
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
		String media = getMediaType(type, genericType);
		if (reader.isReadable(type, genericType, media, con))
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
		if (String.class.equals(type)) {
			if (values != null && values.length > 0)
				return type.cast(values[0]);
			return null;
		}
		if (type.isArray() && String.class.equals(componentType))
			return type.cast(values);
		if (Set.class.equals(type) && String.class.equals(parameterType)) {
			if (values == null)
				return type.cast(Collections.emptySet());
			return type.cast(new HashSet<String>(Arrays.asList(values)));
		}
		if (type.isArray() && isReadable(componentType))
			return type.cast(readArray(componentType));
		if (Set.class.equals(type) && isReadable(parameterType))
			return type.cast(readSet(parameterType));
		if (values != null && values.length > 0)
			return read(values[0], type, genericType);
		return null;
	}

	private <T> T[] readArray(Class<T> componentType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		if (values == null)
			return null;
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
		String media = getMediaType(type, genericType);
		Charset charset = Charset.forName("UTF-16");
		byte[] buf = value.getBytes(charset);
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		return (T) (reader.readFrom(type, genericType, media, in, charset,
				base, null, con));
	}

	private boolean isReadable(Class<?> componentType) {
		String media = getMediaType(componentType, componentType);
		return reader.isReadable(componentType, componentType, media, con);
	}

	private String getMediaType(Class<?> type, Type genericType) {
		if (mediaTypes != null) {
			for (String media : mediaTypes) {
				if (reader.isReadable(type, genericType, media, con))
					return media;
			}
		}
		return mimeType;
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