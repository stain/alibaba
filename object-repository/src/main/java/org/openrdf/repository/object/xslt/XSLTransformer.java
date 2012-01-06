/*
 * Copyright (c) 2009, Zepheira All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.util.ManagedThreadPool;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Applies XSL transformations with the ability to convert the input and output
 * to a variety of formats.
 */
public class XSLTransformer {
	static Executor executor = new ManagedThreadPool("XSLTransformer ", true);

	private final Logger logger = LoggerFactory.getLogger(XSLTransformer.class);
	private final TransformerFactory tfactory;
	private final Templates xslt;
	private final String systemId;
	private final XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private final DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	{
		builder.setNamespaceAware(true);
		try {
			builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			logger.warn(e.toString(), e);
		}
	}

	public XSLTransformer() {
		this(null);
	}

	public XSLTransformer(String url) {
		this.systemId = url;
		tfactory = new CachedTransformerFactory(url);
		xslt = null;
	}

	public XSLTransformer(Reader markup, String systemId) {
		this.systemId = systemId;
		tfactory = new CachedTransformerFactory(systemId);
		ErrorCatcher error = new ErrorCatcher(systemId);
		tfactory.setErrorListener(error);
		try {
			DocumentBuilder db = builder.newDocumentBuilder();
			InputSource is = new InputSource(markup);
			is.setSystemId(systemId);
			Source source = new DOMSource(db.parse(is), systemId);
			xslt = tfactory.newTemplates(source);
			if (error.isFatal())
				throw error.getFatalError();
		} catch (TransformerConfigurationException e) {
			throw new ObjectCompositionException(e);
		} catch (TransformerException e) {
			throw new ObjectCompositionException(e);
		} catch (SAXException e) {
			throw new ObjectCompositionException(e);
		} catch (IOException e) {
			throw new ObjectCompositionException(e);
		} catch (ParserConfigurationException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public String getSystemId() {
		return systemId;
	}

	@Override
	public String toString() {
		return systemId;
	}

	public TransformBuilder transform() throws TransformerException,
			IOException {
		return builder(new DOMSource(), null);
	}

	public TransformBuilder transform(Void nil, String systemId)
			throws TransformerException, IOException {
		return transform();
	}

	public TransformBuilder transform(File file, String systemId)
			throws TransformerException, IOException {
		if (file == null)
			return transform();
		FileInputStream in = new FileInputStream(file);
		return transform(in, systemId);
	}

	public TransformBuilder transform(RDFObject object, String systemId)
			throws IOException, TransformerException {
		if (object == null)
			return transform();
		String uri = object.getResource().stringValue();
		return builder(tfactory.getURIResolver().resolve(uri, null));
	}

	public TransformBuilder transform(URL url, String systemId)
			throws IOException, TransformerException {
		if (url == null)
			return transform();
		return builder(tfactory.getURIResolver().resolve(url.toExternalForm(), null));
	}

	public TransformBuilder transform(String string, String systemId)
			throws TransformerException, IOException {
		if (string == null)
			return transform();
		return transform(new StringReader(string), systemId);
	}

	public TransformBuilder transform(CharSequence string, String systemId)
			throws TransformerException, IOException {
		if (string == null)
			return transform();
		return transform(new StringReader(string.toString()), systemId);
	}

	public TransformBuilder transform(Readable readable, String systemId)
			throws TransformerException, IOException {
		if (readable == null)
			return transform();
		if (readable instanceof Reader)
			return transform((Reader) readable, systemId);
		return transform(new ReadableReader(readable), systemId);
	}

	public TransformBuilder transform(Reader reader, String systemId)
			throws TransformerException, IOException {
		try {
			// Use our own parser to disable loading external DTDs
			DocumentBuilder db = builder.newDocumentBuilder();
			InputSource input = new InputSource(reader);
			input.setSystemId(systemId);
			return builder(new DOMSource(db.parse(input), systemId), null);
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} finally {
			reader.close();
		}
	}

	public TransformBuilder transform(ByteArrayOutputStream buf, String systemId)
			throws TransformerException, IOException {
		if (buf == null)
			return transform();
		return transform(buf.toByteArray(), systemId);
	}

	public TransformBuilder transform(byte[] buf, String systemId)
			throws TransformerException, IOException {
		if (buf == null)
			return transform();
		return transform(new ByteArrayInputStream(buf), systemId);
	}

	public TransformBuilder transform(ReadableByteChannel channel,
			String systemId) throws TransformerException, IOException {
		if (channel == null)
			return transform();
		return transform(Channels.newInputStream(channel), systemId);
	}

	public TransformBuilder transform(InputStream stream, String systemId)
			throws TransformerException, IOException {
		if (stream == null)
			return transform();
		try {
			// Use our own parser to disable loading external DTDs
			DocumentBuilder db = builder.newDocumentBuilder();
			Document doc = db.parse(stream, systemId);
			return builder(new DOMSource(doc, systemId), null);
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} finally {
			stream.close();
		}
	}

	public TransformBuilder transform(final XMLEventReader reader,
			final String systemId) throws XMLStreamException,
			TransformerException, IOException {
		if (reader == null)
			return transform();
		PipedInputStream input = new PipedInputStream();
		final PipedOutputStream output = new PipedOutputStream(input);
		final Exception caught = new Exception();
		executor.execute(new Runnable() {
			public String toString() {
				return "transforming " + systemId;
			}

			public void run() {
				try {
					XMLEventWriter writer = factory
							.createXMLEventWriter(output);
					try {
						writer.add(reader);
					} finally {
						reader.close();
						writer.close();
						output.close();
					}
				} catch (XMLStreamException e) {
					caught.initCause(e);
				} catch (IOException e) {
					caught.initCause(e);
				} catch (Throwable e) {
					caught.initCause(e);
				}
			}
		});
		TransformBuilder builder = transform(input, systemId);
		if (caught.getCause() instanceof IOException)
			throw new IOException((IOException) caught.getCause());
		if (caught.getCause() != null)
			throw new TransformerException(caught.getCause());
		return builder;
	}

	public TransformBuilder transform(Document node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId), null);
	}

	public TransformBuilder transform(DocumentFragment node, String systemId)
			throws TransformerException, IOException,
			ParserConfigurationException {
		if (node == null)
			return transform();
		NodeList nodes = node.getChildNodes();
		if (nodes.getLength() == 1)
			return builder(new DOMSource(node.getFirstChild(), systemId), null);
		Document doc = builder.newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		root.appendChild(doc.importNode(node, true));
		return builder(new DOMSource(root, systemId), null);
	}

	public TransformBuilder transform(Element node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId), null);
	}

	public TransformBuilder transform(Node node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId), null);
	}

	public TransformBuilder transform(final GraphQueryResult result,
			final String systemId) throws TransformerException, IOException {
		if (result == null)
			return transform();
		PipedInputStream input = new PipedInputStream();
		final PipedOutputStream output = new PipedOutputStream(input);
		final Exception caught = new Exception();
		executor.execute(new Runnable() {
			public String toString() {
				return "transforming " + systemId;
			}

			public void run() {
				try {
					RDFXMLWriter writer = new RDFXMLWriter(output);
					try {
						QueryResultUtil.report(result, writer);
					} finally {
						output.close();
					}
				} catch (IOException e) {
					caught.initCause(e);
				} catch (Throwable e) {
					caught.initCause(e);
				}
			}
		});
		TransformBuilder builder = transform(input, systemId);
		if (caught.getCause() instanceof IOException)
			throw new IOException((IOException) caught.getCause());
		if (caught.getCause() != null)
			throw new TransformerException(caught.getCause());
		return builder;
	}

	public TransformBuilder transform(final TupleQueryResult result,
			final String systemId) throws TransformerException, IOException {
		if (result == null)
			return transform();
		PipedInputStream input = new PipedInputStream();
		final PipedOutputStream output = new PipedOutputStream(input);
		final Exception caught = new Exception();
		executor.execute(new Runnable() {
			public String toString() {
				return "transforming " + systemId;
			}

			public void run() {
				try {
					SPARQLResultsXMLWriter writer;
					writer = new SPARQLResultsXMLWriter(output);
					try {
						QueryResultUtil.report(result, writer);
					} finally {
						output.close();
					}
				} catch (IOException e) {
					caught.initCause(e);
				} catch (Throwable e) {
					caught.initCause(e);
				}
			}
		});
		TransformBuilder builder = transform(input, systemId);
		if (caught.getCause() instanceof IOException)
			throw new IOException((IOException) caught.getCause());
		if (caught.getCause() != null)
			throw new TransformerException(caught.getCause());
		return builder;
	}

	public TransformBuilder transform(final Boolean result,
			final String systemId) throws TransformerException, IOException {
		if (result == null)
			return transform();
		PipedInputStream input = new PipedInputStream();
		final PipedOutputStream output = new PipedOutputStream(input);
		final Exception caught = new Exception();
		executor.execute(new Runnable() {
			public String toString() {
				return "transforming " + systemId;
			}

			public void run() {
				try {
					SPARQLBooleanXMLWriter writer;
					writer = new SPARQLBooleanXMLWriter(output);
					try {
						writer.write(result);
					} finally {
						output.close();
					}
				} catch (IOException e) {
					caught.initCause(e);
				} catch (Throwable e) {
					caught.initCause(e);
				}
			}
		});
		TransformBuilder builder = transform(input, systemId);
		if (caught.getCause() instanceof IOException)
			throw new IOException((IOException) caught.getCause());
		if (caught.getCause() != null)
			throw new TransformerException(caught.getCause());
		return builder;
	}

	private TransformBuilder builder(Source source)
			throws TransformerException, IOException {
		Closeable stream = null;
		if (source instanceof StreamSource) {
			StreamSource ss = (StreamSource) source;
			InputStream in = ss.getInputStream();
			if (in == null) {
				stream = ss.getReader();
			} else {
				stream = in;
			}
		}
		return builder(source, stream);
	}

	private TransformBuilder builder(Source source, Closeable closeable)
			throws TransformerException, IOException {
		try {
			TransformBuilder tb = new TransformBuilder(newTransformer(),
					systemId, source, closeable, tfactory.getURIResolver());
			return tb.with("xslt", systemId);
		} catch (RuntimeException e) {
			if (closeable != null) {
				closeable.close();
			}
			throw e;
		} catch (Error e) {
			if (closeable != null) {
				closeable.close();
			}
			throw e;
		} catch (TransformerException e) {
			if (closeable != null) {
				closeable.close();
			}
			throw e;
		}
	}

	private Transformer newTransformer()
			throws TransformerConfigurationException {
		if (xslt != null)
			return xslt.newTransformer();
		if (systemId == null)
			return tfactory.newTransformer();
		StreamSource xsl = new StreamSource(systemId);
		Templates templates = tfactory.newTemplates(xsl);
		if (templates == null)
			return tfactory.newTransformer();
		return templates.newTransformer();
	}

}
