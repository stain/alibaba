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
package org.openrdf.repository.object.managers.helpers;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XSLTransformer {
	private static final Pattern SMAXAGE = Pattern
			.compile("s-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("max-age\\s*=\\s*(\\d+)");
	private static final int XML_EVENT_BUFFER = 10;
	private static final String ACCEPT_XSLT = "application/xslt+xml, text/xsl, application/xml;q=0.8, text/xml;q=0.8, application/*;q=0.6";
	private static Executor executor = Executors.newCachedThreadPool();

	public static class ErrorCatcher implements ErrorListener {
		private Logger logger = LoggerFactory.getLogger(ErrorCatcher.class);
		private TransformerException fatal;
		private IOException io;

		public boolean isFatal() {
			return fatal != null;
		}

		public TransformerException getFatalError() {
			return fatal;
		}

		public boolean isIOException() {
			return io != null;
		}

		public IOException getIOException() {
			return io;
		}

		public void ioException(IOException exception) {
			if (this.io == null) {
				this.io = exception;
			}
			logger.info(exception.toString(), exception);
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

	private static class ReadableReader extends Reader {
		private final Readable reader;

		private ReadableReader(Readable reader) {
			this.reader = reader;
		}

		@Override
		public void close() throws IOException {
			if (reader instanceof Closeable) {
				((Closeable) reader).close();
			}
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return reader.read(CharBuffer.wrap(cbuf, off, len));
		}

		@Override
		public int read(CharBuffer cbuf) throws IOException {
			return reader.read(cbuf);
		}
	}

	public class TransformBuilder {
		private Source source;
		private Transformer transformer;
		private ErrorCatcher listener;

		public TransformBuilder(Transformer transformer, Source source) {
			this.transformer = transformer;
			listener = new ErrorCatcher();
			transformer.setErrorListener(listener);
			this.source = source;
		}

		protected void fatalError(TransformerException exception) {
			listener.fatalError(exception);
		}

		public TransformBuilder with(String name, String value) {
			transformer.setParameter(name, value);
			return this;
		}

		public String asString() throws IOException {
			return asCharSequence().toString();
		}

		public CharSequence asCharSequence() throws IOException {
			StringWriter output = new StringWriter();
			transform(new StreamResult(output), output);
			if (listener.isIOException())
				throw listener.getIOException();
			return output.getBuffer();
		}

		public Readable asReadable() throws IOException {
			return asReader();
		}

		public byte[] asByteArray() throws IOException {
			return asByteArrayOutputStream().toByteArray();
		}

		public ReadableByteChannel asReadableByteChannel() throws IOException {
			return Channels.newChannel(asInputStream());
		}

		public ByteArrayOutputStream asByteArrayOutputStream()
				throws IOException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			transform(new StreamResult(output), output);
			if (listener.isIOException())
				throw listener.getIOException();
			return output;
		}

		public Document asDocument() throws TransformerException, IOException,
				ParserConfigurationException {
			return (Document) asNode();
		}

		public DocumentFragment asDocumentFragment()
				throws TransformerException, IOException,
				ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			DOMResult output = new DOMResult(frag);
			transform(output);
			return frag;
		}

		public Element asElement() throws TransformerException, IOException,
				ParserConfigurationException {
			return asDocument().getDocumentElement();
		}

		public Node asNode() throws TransformerException, IOException,
				ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DOMResult output = new DOMResult(doc);
			transform(output);
			return output.getNode();
		}

		public XMLEventReader asXMLEventReader() throws IOException {
			final XMLEventQueue queue = new XMLEventQueue(XML_EVENT_BUFFER);
			executor.execute(new Runnable() {
				public void run() {
					transform(queue);
				}
			});
			return queue.getXMLEventReader();
		}

		public InputStream asInputStream() throws IOException {
			final PipedOutputStream output = new PipedOutputStream();
			PipedInputStream input = new PipedInputStream(output) {
				public void close() throws IOException {
					try {
						if (listener.isIOException())
							throw listener.getIOException();
					} finally {
						super.close();
					}
				}
			};
			executor.execute(new Runnable() {
				public void run() {
					transform(new StreamResult(output), output);
				}
			});
			return input;
		}

		public Reader asReader() throws IOException {
			final PipedWriter output = new PipedWriter();
			PipedReader input = new PipedReader(output) {
				public void close() throws IOException {
					try {
						if (listener.isIOException())
							throw listener.getIOException();
					} finally {
						super.close();
					}
				}
			};
			executor.execute(new Runnable() {
				public void run() {
					transform(new StreamResult(output), output);
				}
			});
			return input;
		}

		private void transform(DOMResult output) throws TransformerException,
				IOException {
			transform(output, null);
			if (listener.isIOException())
				throw listener.getIOException();
		}

		private void transform(XMLEventQueue queue) {
			try {
				Result result = new StAXResult(queue);
				transformer.transform(source, result);
				if (listener.isFatal())
					throw listener.getFatalError();
			} catch (TransformerException e) {
				queue.abort(new XMLStreamException(e));
			} finally {
				try {
					queue.close();
				} catch (XMLStreamException e) {
					queue.abort(e);
				}
			}
		}

		private void transform(Result result, Closeable output) {
			try {
				transformer.transform(source, result);
				if (listener.isFatal())
					throw listener.getFatalError();
			} catch (TransformerException e) {
				listener.ioException(new IOException(e));
			} finally {
				try {
					if (output != null) {
						output.close();
					}
				} catch (IOException e) {
					listener.ioException(e);
				}
			}
		}
	}

	private URL url;
	private Templates xslt;
	private String tag;
	private Integer maxage;
	private long expires;
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	{
		builder.setNamespaceAware(true);
	}

	public XSLTransformer(String url) throws TransformerException, IOException {
		this.url = new java.net.URL(url);
		HttpURLConnection con = (HttpURLConnection) this.url.openConnection();
		try {
			con.addRequestProperty("Accept", ACCEPT_XSLT);
			con.addRequestProperty("Accept-Encoding", "gzip");
			if (isStorable(con.getHeaderField("Cache-Control"))) {
				xslt = newTemplates(con);
			}
		} finally {
			con.disconnect();
		}
	}

	public XSLTransformer(Reader markup, String systemId)
			throws TransformerException {
		TransformerFactory factory = TransformerFactory.newInstance();
		ErrorCatcher error = new ErrorCatcher();
		factory.setErrorListener(error);
		Source source = new StreamSource(markup, systemId);
		xslt = factory.newTemplates(source);
		if (error.isFatal())
			throw error.getFatalError();
	}

	public TransformBuilder transform() throws TransformerException,
			IOException {
		return builder(new DOMSource());
	}

	public TransformBuilder transform(File file, String systemId)
			throws TransformerException, IOException {
		if (file == null)
			return transform();
		return builder(new StreamSource(new FileInputStream(file), systemId));
	}

	public TransformBuilder transform(RDFObject object, String systemId)
			throws IOException, TransformerException {
		if (object == null)
			return transform();
		String uri = object.getResource().stringValue();
		return builder(new StreamSource(uri));
	}

	public TransformBuilder transform(URL url, String systemId)
			throws IOException, TransformerException {
		if (url == null)
			return transform();
		return builder(new StreamSource(url.toExternalForm()));
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
		return builder(new StreamSource(reader, systemId));
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
		return builder(new StreamSource(stream, systemId));
	}

	public TransformBuilder transform(final XMLEventReader reader,
			String systemId) throws XMLStreamException, TransformerException,
			IOException {
		if (reader == null)
			return transform();
		Source source;
		try {
			source = new StAXSource(reader);
		} catch (NullPointerException e) {
			// no location
			PipedInputStream input = new PipedInputStream();
			final PipedOutputStream output = new PipedOutputStream(input);
			final TransformBuilder builder = transform(input, systemId);
			executor.execute(new Runnable() {
				public void run() {
					try {
						XMLEventWriter writer = factory
								.createXMLEventWriter(output);
						try {
							writer.add(reader);
						} finally {
							writer.close();
							output.close();
						}
					} catch (XMLStreamException e) {
						builder.fatalError(new TransformerException(e));
					} catch (IOException e) {
						builder.fatalError(new TransformerException(e));
					}
				}
			});
			return builder;
		}
		return builder(source);
	}

	public TransformBuilder transform(Document node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId));
	}

	public TransformBuilder transform(DocumentFragment node, String systemId)
			throws TransformerException, IOException,
			ParserConfigurationException {
		if (node == null)
			return transform();
		NodeList nodes = node.getChildNodes();
		if (nodes.getLength() == 1)
			return builder(new DOMSource(node.getFirstChild(), systemId));
		Document doc = builder.newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		root.appendChild(doc.importNode(node, true));
		return builder(new DOMSource(root, systemId));
	}

	public TransformBuilder transform(Element node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId));
	}

	public TransformBuilder transform(Node node, String systemId)
			throws TransformerException, IOException {
		if (node == null)
			return transform();
		return builder(new DOMSource(node, systemId));
	}

	private TransformBuilder builder(Source source)
			throws TransformerException, IOException {
		return new TransformBuilder(newTransformer(), source);
	}

	private synchronized Transformer newTransformer()
			throws TransformerException, IOException {
		if (xslt != null && (expires == 0 || expires > currentTimeMillis()))
			return xslt.newTransformer();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		try {
			con.addRequestProperty("Accept", ACCEPT_XSLT);
			con.addRequestProperty("Accept-Encoding", "gzip");
			if (tag != null) {
				con.addRequestProperty("If-None-Match", tag);
			}
			if (isStorable(con.getHeaderField("Cache-Control"))) {
				xslt = newTemplates(con);
				return xslt.newTransformer();
			} else {
				xslt = null;
				tag = null;
				expires = 0;
				maxage = 0;
				return newTemplates(con).newTransformer();
			}
		} finally {
			con.disconnect();
		}
	}

	private boolean isStorable(String cc) {
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private Templates newTemplates(HttpURLConnection con) throws IOException,
			TransformerException {
		String cacheControl = con.getHeaderField("Cache-Control");
		long date = con.getHeaderFieldDate("Expires", expires);
		expires = getExpires(cacheControl, date);
		int status = con.getResponseCode();
		if (status == 304 || status == 412)
			return xslt; // Not Modified
		tag = con.getHeaderField("ETag");
		String encoding = con.getHeaderField("Content-Encoding");
		InputStream in = con.getInputStream();
		if (encoding != null && encoding.contains("gzip")) {
			in = new GZIPInputStream(in);
		}
		TransformerFactory factory = TransformerFactory.newInstance();
		ErrorCatcher error = new ErrorCatcher();
		factory.setErrorListener(error);
		try {
			String base = con.getURL().toExternalForm();
			Source source = new StreamSource(in, base);
			return factory.newTemplates(source);
		} finally {
			if (error.isFatal())
				throw error.getFatalError();
		}
	}

	private long getExpires(String cacheControl, long defaultValue) {
		if (cacheControl != null && cacheControl.contains("s-maxage")) {
			try {
				Matcher m = SMAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		} else if (cacheControl != null && cacheControl.contains("max-age")) {
			try {
				Matcher m = MAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		if (maxage != null)
			return currentTimeMillis() + maxage * 1000;
		return defaultValue;
	}

}
