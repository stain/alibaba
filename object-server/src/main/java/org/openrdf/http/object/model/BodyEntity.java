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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.http.object.exceptions.UnsupportedMediaType;
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
 * Wraps messages readers for a set of headers.
 */
public abstract class BodyEntity implements Entity {
	private MessageBodyReader reader = AggregateReader.getInstance();
	private String mimeType;
	private boolean stream;
	private Charset charset;
	private String base;
	private String location;
	private ObjectConnection con;

	public BodyEntity(String mimeType, boolean stream, Charset charset,
			String base, String location, ObjectConnection con) {
		this.mimeType = mimeType;
		this.stream = stream;
		this.charset = charset;
		this.base = base;
		this.location = location;
		this.con = con;
	}

	public Collection<MimeType> getReadableTypes(Class<?> ctype, Type gtype,
			Accepter accepter) throws MimeTypeParseException {
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType media : accepter.getAcceptable(mimeType)) {
			if (!stream && location == null) {
				acceptable.add(media);
				continue; // reads null
			}
			if (stream && ReadableByteChannel.class.equals(ctype)) {
				acceptable.add(media);
				continue;
			}
			if (reader.isReadable(ctype, gtype, media.toString(), con)) {
				acceptable.add(media);
				continue;
			}
			GenericType<?> type = new GenericType(ctype, gtype);
			if (type.isSetOrArray()) {
				Type cgtype = type.getComponentType();
				Class<?> cctype = type.getComponentClass();
				if (reader.isReadable(cctype, cgtype, media.toString(), con)) {
					acceptable.add(media);
					continue;
				}
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> ctype, Type gtype, String[] mediaTypes)
			throws MimeTypeParseException, IOException,
			QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		GenericType<T> type = new GenericType(ctype, gtype);
		if (location == null && !stream)
			return null;
		ReadableByteChannel in = getReadableByteChannel();
		if (stream && type.isOrIsSetOf(ReadableByteChannel.class))
			return type.castComponent(in);
		for (MimeType media : new Accepter(mediaTypes).getAcceptable(mimeType)) {
			if (!reader.isReadable(ctype, gtype, media.toString(), con))
				continue;
			return (T) (reader.readFrom(ctype, gtype, media.toString(), in,
					charset, base, location, con));
		}
		if (reader.isReadable(ctype, gtype, mimeType.toString(), con))
			return (T) (reader.readFrom(ctype, gtype, mimeType, in, charset, base,
					location, con));
		if (type.isSetOrArray()) {
			Type cgtype = type.getComponentType();
			Class<?> cctype = type.getComponentClass();
			for (MimeType media : new Accepter(mediaTypes)
					.getAcceptable(mimeType)) {
				if (!reader.isReadable(cctype, cgtype, media.toString(), con))
					continue;
				return type.castComponent(reader.readFrom(cctype, cgtype, media
						.toString(), in, charset, base, location, con));
			}
			if (reader.isReadable(cctype, cgtype, mimeType.toString(), con))
				return type.castComponent(reader.readFrom(cctype, cgtype,
						mimeType, in, charset, base, location, con));
		}
		throw new UnsupportedMediaType();
	}

	protected abstract ReadableByteChannel getReadableByteChannel()
			throws IOException;

}
