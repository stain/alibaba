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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Parses a DOM Node from an InputStream.
 */
public class DOMMessageReader implements MessageBodyReader<Node> {

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
		if (mediaType != null && !mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/")
				&& !mediaType.contains("xml"))
			return false;
		return type.isAssignableFrom(Document.class)
				|| type.isAssignableFrom(Element.class)
				|| type.isAssignableFrom(DocumentFragment.class)
				&& (mediaType == null || !mediaType.startsWith("text/"));
	}

	public Node readFrom(Class<?> type, Type genericType, String mimeType,
			InputStream in, Charset charset, String base, String location,
			ObjectConnection con) throws TransformerConfigurationException,
			TransformerException, ParserConfigurationException {
		Node node = createNode(type);
		DOMResult result = new DOMResult(node);
		Source source = createSource(location, in, charset);
		Transformer transformer = factory.newTransformer();
		ErrorCatcher listener = new ErrorCatcher();
		transformer.setErrorListener(listener);
		transformer.transform(source, result);
		if (listener.isFatal())
			throw listener.getFatalError();
		if (type.isAssignableFrom(node.getClass()))
			return node;
		return ((Document) node).getDocumentElement();
	}

	private Node createNode(Class<?> type) throws ParserConfigurationException {
		Document doc = builder.newDocumentBuilder().newDocument();
		if (type.isAssignableFrom(doc.getClass()))
			return doc;
		if (type.isAssignableFrom(DocumentFragment.class))
			return doc.createDocumentFragment();
		return doc;
	}

	private Source createSource(String location, InputStream in, Charset charset) {
		if (charset == null && in != null && location != null)
			return new StreamSource(in, location);
		if (charset == null && in != null && location == null)
			return new StreamSource(in);
		if (in == null && location != null)
			return new StreamSource(location);
		Reader reader = new InputStreamReader(in, charset);
		if (location != null)
			return new StreamSource(reader, location);
		return new StreamSource(reader);
	}
}
