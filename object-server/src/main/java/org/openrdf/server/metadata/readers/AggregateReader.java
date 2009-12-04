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
package org.openrdf.server.metadata.readers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.xml.sax.SAXException;

/**
 * Delegates to other {@link MessageBodyReader}.
 * 
 * @author James Leigh
 * 
 */
public class AggregateReader implements MessageBodyReader<Object> {
	private static AggregateReader instance = new AggregateReader();

	public static AggregateReader getInstance() {
		return instance;
	}

	private List<MessageBodyReader> readers = new ArrayList<MessageBodyReader>();

	public AggregateReader() {
		readers.add(new ModelMessageReader());
		readers.add(new GraphMessageReader());
		readers.add(new TupleMessageReader());
		readers.add(new BooleanMessageReader());
		readers.add(new DatatypeReader());
		readers.add(new RDFObjectReader());
		readers.add(new SetOfRDFObjectReader());
		readers.add(new StringBodyReader());
		readers.add(new PrimitiveBodyReader());
		readers.add(new FormMapMessageReader());
		readers.add(new InputStreamBodyReader());
		readers.add(new ReadableBodyReader());
		readers.add(new ReadableByteChannelBodyReader());
		readers.add(new XMLEventMessageReader());
		readers.add(new ByteArrayMessageReader());
		readers.add(new ByteArrayStreamMessageReader());
		readers.add(new DOMMessageReader());
		readers.add(new DocumentFragmentMessageReader());
		readers.add(new URIReader());
		readers.add(new URLReader());
		readers.add(new NetURIReader());
	}

	public boolean isReadable(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		return findReader(type, genericType, mimeType, con) != null;
	}

	public Object readFrom(Class<? extends Object> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, IOException, RepositoryException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		MessageBodyReader reader = findReader(type, genericType, mimeType, con);
		if (reader == null)
			throw new BadRequest("Cannot read " + mimeType + " into " + type);
		return reader.readFrom(type, genericType, mimeType, in, charset, base,
				location, con);
	}

	private MessageBodyReader findReader(Class<?> type, Type genericType,
			String mime, ObjectConnection con) {
		for (MessageBodyReader reader : readers) {
			if (reader.isReadable(type, genericType, mime, con)) {
				return reader;
			}
		}
		return null;
	}

}
