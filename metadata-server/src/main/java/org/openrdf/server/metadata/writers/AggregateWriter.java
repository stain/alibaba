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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectFactory;

/**
 * Delegates to other {@link MessageBodyWriter}s.
 * 
 * @author James Leigh
 * 
 */
public class AggregateWriter implements MessageBodyWriter<Object> {
	private List<MessageBodyWriter> writers = new ArrayList<MessageBodyWriter>();

	public AggregateWriter() throws TransformerConfigurationException {
		writers.add(new FileBodyWriter());
		writers.add(new BooleanMessageWriter());
		writers.add(new ModelMessageWriter());
		writers.add(new GraphMessageWriter());
		writers.add(new TupleMessageWriter());
		writers.add(new RDFObjectWriter());
		writers.add(new SetOfRDFObjectWriter());
		writers.add(new StringBodyWriter());
		writers.add(new InputStreamBodyWriter());
		writers.add(new ReadableBodyWriter());
		writers.add(new ReadableByteChannelBodyWriter());
		writers.add(new XMLEventMessageWriter());
		writers.add(new ByteArrayMessageWriter());
		writers.add(new ByteArrayStreamMessageWriter());
		writers.add(new DOMMessageWriter());
		writers.add(new DocumentFragmentMessageWriter());
	}

	public String getContentType(String mimeType, Class<?> type,
			ObjectFactory of, Charset charset) {
		return findWriter(mimeType, type, of).getContentType(mimeType, type,
				of, charset);
	}

	public long getSize(String mimeType, Class<?> type, ObjectFactory of,
			Object result, Charset charset) {
		return findWriter(mimeType, type, of).getSize(mimeType, type, of,
				result, charset);
	}

	public boolean isWriteable(String mimeType, Class<?> type, ObjectFactory of) {
		return findWriter(mimeType, type, of) != null;
	}

	public void writeTo(String mimeType, Class<?> type, ObjectFactory of,
			Object result, String base, Charset charset, OutputStream out,
			int bufSize) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		MessageBodyWriter writer = findWriter(mimeType, type, of);
		writer.writeTo(mimeType, type, of, result, base, charset, out, bufSize);
	}

	private MessageBodyWriter findWriter(String mimeType, Class<?> type,
			ObjectFactory of) {
		for (MessageBodyWriter w : writers) {
			if (w.isWriteable(mimeType, type, of)) {
				return w;
			}
		}
		return null;
	}

}
