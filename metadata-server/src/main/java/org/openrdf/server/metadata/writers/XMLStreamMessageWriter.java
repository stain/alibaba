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
package org.openrdf.server.metadata.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openrdf.repository.object.ObjectFactory;

public class XMLStreamMessageWriter implements
		MessageBodyWriter<XMLStreamReader> {
	private XMLInputFactory factory = XMLInputFactory.newInstance();
	private XMLEventMessageWriter delegate = new XMLEventMessageWriter();

	public boolean isWriteable(String mediaType, Class<?> type, ObjectFactory of) {
		if (!XMLStreamReader.class.isAssignableFrom(type))
			return false;
		return delegate.isWriteable(mediaType, XMLEventReader.class, of);
	}

	public String getContentType(String mimeType, Class<?> type,
			ObjectFactory of, Charset charset) {
		return delegate.getContentType(mimeType, XMLEventReader.class, of,
				charset);
	}

	public long getSize(String mimeType, Class<?> type, ObjectFactory of,
			XMLStreamReader t, Charset charset) {
		return -1;
	}

	public void writeTo(String mimeType, Class<?> type, ObjectFactory of,
			XMLStreamReader result, String base, Charset charset,
			OutputStream out, int bufSize) throws IOException,
			XMLStreamException {
		XMLEventReader events = factory.createXMLEventReader(result);
		delegate.writeTo(mimeType, XMLEventReader.class, of, events, base,
				charset, out, bufSize);
	}
}
