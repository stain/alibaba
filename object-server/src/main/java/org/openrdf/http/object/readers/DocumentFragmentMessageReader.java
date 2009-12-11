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
package org.openrdf.http.object.readers;

import info.aduna.io.PushbackReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses a DocumentFragment from an InputStream.
 */
public class DocumentFragmentMessageReader implements
		MessageBodyReader<DocumentFragment> {

	private static class ErrorCatcher implements ErrorListener {
		private Logger logger = LoggerFactory.getLogger(ErrorCatcher.class);
		private TransformerException fatal;

		public boolean isFatal() {
			return fatal != null;
		}

		public TransformerException getFatalError() {
			return fatal;
		}

		public void error(TransformerException exception) {
			logger.warn(exception.toString(), exception);
		}

		public void fatalError(TransformerException exception) {
			if (this.fatal == null) {
				this.fatal = exception;
			}
			logger.error(exception.toString(), exception);
		}

		public void warning(TransformerException exception) {
			logger.info(exception.toString(), exception);
		}
	}

	private TransformerFactory factory = TransformerFactory.newInstance();
	private DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	{
		builder.setNamespaceAware(true);
	}

	public boolean isReadable(Class<?> type, Type genericType,
			String mediaType, ObjectConnection con) {
		if (mediaType == null || !mediaType.startsWith("text/"))
			return false;
		return type.isAssignableFrom(DocumentFragment.class);
	}

	public DocumentFragment readFrom(Class<?> type, Type genericType,
			String mimeType, InputStream in, Charset charset, String base,
			String location, ObjectConnection con)
			throws ParserConfigurationException, IOException,
			TransformerException {
		if (charset == null) {
			charset = Charset.forName("US-ASCII");
		}
		Document doc = builder.newDocumentBuilder().newDocument();
		DOMResult result = new DOMResult(doc);
		Transformer transformer = factory.newTransformer();
		ErrorCatcher listener = new ErrorCatcher();
		transformer.setErrorListener(listener);
		Reader reader = new InputStreamReader(in, charset);
		reader = new BufferedReader(reader);
		boolean full = isDocument(reader);
		if (full) {
			transformer.transform(new StreamSource(reader), result);
		} else {
			PushbackReader pushback = new PushbackReader();
			pushback.pushback("</wrapper>");
			pushback.pushback(reader);
			pushback.pushback("<wrapper>");
			transformer.transform(new StreamSource(pushback), result);
		}
		if (listener.isFatal())
			throw listener.getFatalError();
		DocumentFragment frag = doc.createDocumentFragment();
		if (full) {
			frag.appendChild(doc.getDocumentElement());
		} else {
			NodeList nodes = doc.getDocumentElement().getChildNodes();
			int length = nodes.getLength();
			List<Node> list = new ArrayList<Node>(length);
			for (int i = 0; i < length; i++) {
				list.add(nodes.item(i));
			}
			for (Node node : list) {
				frag.appendChild(node);
			}
			doc.removeChild(doc.getDocumentElement());
		}
		return frag;
	}

	private boolean isDocument(Reader reader) throws IOException {
		char[] cbuf = new char[2];
		reader.mark(cbuf.length);
		try {
			return reader.read(cbuf) == 2 && cbuf[0] == '<' && cbuf[1] == '?';
		} finally {
			reader.reset();
		}
	}
}
