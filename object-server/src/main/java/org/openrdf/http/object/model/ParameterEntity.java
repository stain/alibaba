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
package org.openrdf.http.object.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.http.object.readers.AggregateReader;
import org.openrdf.http.object.readers.MessageBodyReader;
import org.openrdf.http.object.util.Accepter;
import org.openrdf.http.object.util.GenericType;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Provides an entity interface for a query parameter.
 */
public class ParameterEntity implements Entity {
	private MessageBodyReader reader = AggregateReader.getInstance();
	private String[] values;
	private String base;
	private ObjectConnection con;
	private String[] mediaTypes;

	public ParameterEntity(String[] mediaTypes, String mimeType,
			String[] values, String base, ObjectConnection con) {
		if (mediaTypes == null || mediaTypes.length == 0) {
			this.mediaTypes = new String[] { mimeType };
		} else {
			this.mediaTypes = mediaTypes;
		}
		this.values = values;
		this.base = base;
		this.con = con;
	}

	public boolean isReadable(Class<?> ctype, Type gtype,
			String[] mediaTypes) throws MimeTypeParseException {
		Accepter accepter = new Accepter(mediaTypes);
		if (!accepter.isAcceptable(this.mediaTypes))
			return false;
		GenericType<?> type = new GenericType(ctype, gtype);
		Class<?> componentType = type.getComponentClass();
		if (type.is(String.class))
			return true;
		if (type.isSetOrArrayOf(String.class))
			return true;
		if (type.isSetOrArray() && isReadable(componentType, mediaTypes))
			return true;
		String media = getMediaType(ctype, gtype, mediaTypes);
		if (reader.isReadable(ctype, gtype, media, con))
			return true;
		return false;
	}

	public <T> T read(Class<T> ctype, Type genericType, String[] mediaTypes)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			MimeTypeParseException {
		GenericType<T> type = new GenericType<T>(ctype, genericType);
		if (type.is(String.class)) {
			if (values != null && values.length > 0)
				return type.cast(values[0]);
			return null;
		}
		if (type.isSetOrArrayOf(String.class)) {
			return type.castArray(values);
		}
		Class<?> componentType = type.getComponentClass();
		if (type.isArray() && isReadable(componentType, mediaTypes))
			return type.castArray(readArray(componentType, mediaTypes));
		if (type.isSet() && isReadable(componentType, mediaTypes))
			return type.castSet(readSet(componentType, mediaTypes));
		if (values != null && values.length > 0)
			return read(values[0], ctype, genericType, mediaTypes);
		return null;
	}

	private <T> T[] readArray(Class<T> componentType, String[] mediaTypes)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			MimeTypeParseException {
		if (values == null)
			return null;
		T[] result = (T[]) Array.newInstance(componentType, values.length);
		for (int i = 0; i < values.length; i++) {
			result[i] = read(values[i], componentType, componentType,
					mediaTypes);
		}
		return result;
	}

	private <T> Set<T> readSet(Class<T> componentType, String[] mediaTypes)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			MimeTypeParseException {
		Set<T> result = new LinkedHashSet<T>(values.length);
		for (int i = 0; i < values.length; i++) {
			result
					.add(read(values[i], componentType, componentType,
							mediaTypes));
		}
		return result;
	}

	private <T> T read(String value, Class<T> type, Type genericType,
			String... mediaTypes) throws QueryResultParseException,
			TupleQueryResultHandlerException, QueryEvaluationException,
			IOException, RepositoryException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			MimeTypeParseException {
		String media = getMediaType(type, genericType, mediaTypes);
		Charset charset = Charset.forName("UTF-16");
		byte[] buf = value.getBytes(charset);
		ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(buf));
		return (T) (reader.readFrom(type, genericType, media, in, charset,
				base, null, con));
	}

	private boolean isReadable(Class<?> componentType, String[] mediaTypes)
			throws MimeTypeParseException {
		String media = getMediaType(componentType, componentType, mediaTypes);
		return reader.isReadable(componentType, componentType, media, con);
	}

	private String getMediaType(Class<?> type, Type genericType,
			String[] mediaTypes) throws MimeTypeParseException {
		Accepter accepter = new Accepter(mediaTypes);
		for (MimeType m : accepter.getAcceptable(this.mediaTypes)) {
			if (reader.isReadable(type, genericType, m.toString(), con))
				return m.toString();
		}
		return null;
	}

}
