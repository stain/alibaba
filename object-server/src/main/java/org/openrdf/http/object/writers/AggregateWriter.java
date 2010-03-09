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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.writers.base.URIListWriter;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectFactory;

/**
 * Delegates to other {@link MessageBodyWriter}s.
 * 
 * @author James Leigh
 * 
 */
public class AggregateWriter implements MessageBodyWriter<Object> {
	private static AggregateWriter instance = new AggregateWriter();
	static {
		try {
			instance.init();
		} catch (TransformerConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	public static AggregateWriter getInstance() {
		return instance;
	}

	private List<MessageBodyWriter> writers = new ArrayList<MessageBodyWriter>();

	private AggregateWriter() {
		super();
	}

	private void init() throws TransformerConfigurationException {
		writers.add(new URIListWriter(URI.class));
		writers.add(new URIListWriter(URL.class));
		writers.add(new URIListWriter(java.net.URI.class));
		writers.add(new RDFObjectURIWriter());
		writers.add(new BooleanMessageWriter());
		writers.add(new ModelMessageWriter());
		writers.add(new GraphMessageWriter());
		writers.add(new TupleMessageWriter());
		writers.add(new DatatypeWriter());
		writers.add(new RDFObjectWriter());
		writers.add(new SetOfRDFObjectWriter());
		writers.add(new StringBodyWriter());
		writers.add(new PrimitiveBodyWriter());
		writers.add(new InputStreamBodyWriter());
		writers.add(new ReadableBodyWriter());
		writers.add(new ReadableByteChannelBodyWriter());
		writers.add(new XMLEventMessageWriter());
		writers.add(new ByteArrayMessageWriter());
		writers.add(new ByteArrayStreamMessageWriter());
		writers.add(new DOMMessageWriter());
		writers.add(new DocumentFragmentMessageWriter());
		writers.add(new FormMapMessageWriter());
		writers.add(new FormStringMessageWriter());
		writers.add(new HttpMessageWriter());
	}

	public String getContentType(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of, Charset charset) {
		return findWriter(mimeType, type, genericType, of).getContentType(
				mimeType, type, genericType, of, charset);
	}

	public long getSize(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Object result, Charset charset) {
		return findWriter(mimeType, type, genericType, of).getSize(mimeType,
				type, genericType, of, result, charset);
	}

	public boolean isWriteable(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		return findWriter(mimeType, type, genericType, of) != null;
	}

	public InputStream write(String mimeType, Class<?> type, Type genericType,
			ObjectFactory of, Object result, String base, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		MessageBodyWriter writer = findWriter(mimeType, type, genericType, of);
		if (writer == null)
			throw new BadRequest("Cannot write " + type + " into " + mimeType);
		return writer.write(mimeType, type, genericType, of, result, base, charset);
	}

	private MessageBodyWriter findWriter(String mimeType, Class<?> type,
			Type genericType, ObjectFactory of) {
		for (MessageBodyWriter w : writers) {
			if (w.isWriteable(mimeType, type, genericType, of)) {
				return w;
			}
		}
		return null;
	}

}
