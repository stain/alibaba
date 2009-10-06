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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

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

public class BodyEntity implements Entity {
	private MessageBodyReader reader;
	private String mimeType;
	private InputStream in;
	private Charset charset;
	private String base;
	private String location;
	private ObjectConnection con;

	public BodyEntity(MessageBodyReader reader, String mimeType,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) {
		this.reader = reader;
		this.mimeType = mimeType;
		this.in = in;
		this.charset = charset;
		this.base = base;
		this.location = location;
		this.con = con;
	}

	public void close() throws IOException {
		if (in != null) {
			in.close();
		}
	}

	public boolean isReadable(Class<?> type, Type genericType) {
		if (InputStream.class.equals(type))
			return true;
		return reader.isReadable(type, genericType, mimeType, con);
	}

	public <T> T read(Class<T> type, Type genericType)
			throws QueryResultParseException, TupleQueryResultHandlerException,
			QueryEvaluationException, RepositoryException,
			TransformerConfigurationException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		if (InputStream.class.equals(type))
			return type.cast(in);
		if (location == null && in == null)
			return null;
		return (T) (reader.readFrom(type, genericType, mimeType, in, charset,
				base, location, con));
	}

}
