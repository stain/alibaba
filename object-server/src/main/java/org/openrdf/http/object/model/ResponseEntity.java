/*
 * Copyright 2009-2010, Zepheira LLC Some rights reserved.
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

import static java.util.Collections.singleton;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.readers.AggregateReader;
import org.openrdf.http.object.readers.MessageBodyReader;
import org.openrdf.http.object.util.Accepter;
import org.openrdf.http.object.util.GenericType;
import org.openrdf.http.object.writers.AggregateWriter;
import org.openrdf.http.object.writers.MessageBodyWriter;
import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.xml.sax.SAXException;

/**
 * Wraps a message response to output to an HTTP response.
 */
public class ResponseEntity implements Entity {
	private MessageBodyWriter writer = AggregateWriter.getInstance();
	private MessageBodyReader reader = AggregateReader.getInstance();
	private String[] mimeTypes;
	private Object result;
	private Class<?> type;
	private Type genericType;
	private String base;
	private ObjectConnection con;
	private ObjectFactory of;

	public ResponseEntity(String[] mimeTypes, Object result, Class<?> type,
			Type genericType, String base, ObjectConnection con) {
		this.mimeTypes = mimeTypes;
		this.result = result;
		this.type = type;
		this.genericType = genericType;
		this.base = base;
		this.con = con;
		this.of = con == null ? null : con.getObjectFactory();
		if (mimeTypes == null || mimeTypes.length < 1) {
			this.mimeTypes = new String[] { "*/*" };
		}
	}

	public String toString() {
		return String.valueOf(result);
	}

	public Object getEntity() {
		return result;
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> type, Type genericType,
			Accepter accepter) throws MimeTypeParseException {
		if (!accepter.isAcceptable(mimeTypes))
			return Collections.emptySet();
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return accepter.getAcceptable();
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType mimeType : accepter.getAcceptable(mimeTypes)) {
			if (isWriteable(mimeType.toString())) {
				String contentType = getContentType(mimeType.toString());
				String mime = removeParamaters(contentType);
				if (isReadable(type, genericType, mime)) {
					acceptable.add(mimeType);
				}
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> type, Type genericType, String[] mediaTypes)
			throws OpenRDFException, TransformerConfigurationException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException {
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return (T) (result);
		Accepter accepter = new Accepter(mediaTypes);
		for (final MimeType mimeType : accepter.getAcceptable(mimeTypes)) {
			if (isWriteable(mimeType.toString())) {
				String contentType = getContentType(mimeType.toString());
				String mime = removeParamaters(contentType);
				Charset charset = getCharset(contentType);
				if (isReadable(type, genericType, mime)) {
					ReadableByteChannel in = write(mimeType.toString(), null);
					return (T) (readFrom(type, genericType, mime, charset, in));
				}
			}
		}
		throw new ClassCastException(String.valueOf(result)
				+ " cannot be converted into " + type.getSimpleName());
	}

	public boolean isNoContent() {
		return result == null || Set.class.equals(type)
				&& ((Set) result).isEmpty();
	}

	public Set<String> getLocations() {
		if (result instanceof URI)
			return singleton(((URI) result).stringValue());
		if (result instanceof RDFObject)
			return singleton(((RDFObject) result).getResource().stringValue());
		if (result instanceof Set) {
			GenericType<?> gtype = new GenericType(type, genericType);
			if (gtype.isSet()) {
				Set set = (Set) result;
				Iterator iter = set.iterator();
				try {
					Set<String> locations = new LinkedHashSet<String>();
					while (iter.hasNext()) {
						Object object = iter.next();
						if (object instanceof RDFObject) {
							locations.add(((RDFObject) object).getResource()
									.stringValue());
						} else if (object instanceof URI) {
							locations.add(((URI) object).stringValue());
						}
					}
					return locations;
				} finally {
					ObjectConnection.close(iter);
				}
			}
		}
		return null;
	}

	public long getSize(String mimeType, Charset charset) {
		return writer.getSize(mimeType, type, genericType, of, result, charset);
	}

	public ReadableByteChannel write(String mimeType, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		return writer.write(mimeType, type, genericType, of, result, base,
				charset);
	}

	private boolean isReadable(Class<?> type, Type genericType, String mime) {
		return reader.isReadable(type, genericType, mime, con);
	}

	private boolean isWriteable(String mimeType) {
		return writer.isWriteable(mimeType, type, genericType, of);
	}

	private <T> Object readFrom(Class<T> type, Type genericType, String mime,
			Charset charset, ReadableByteChannel in) throws QueryResultParseException,
			TupleQueryResultHandlerException, QueryEvaluationException,
			IOException, RepositoryException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		return reader.readFrom(type, genericType, mime, in, charset, base,
				null, con);
	}

	private String getContentType(String mimeType) {
		return writer.getContentType(mimeType, type, genericType, of, null);
	}

	private String removeParamaters(String mediaType) {
		if (mediaType == null)
			return null;
		int idx = mediaType.indexOf(';');
		if (idx > 0)
			return mediaType.substring(0, idx);
		return mediaType;
	}

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

}
