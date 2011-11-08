/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper class to run XSLT with parameters.
 *
 * @author James Leigh
 */
public class TransformBuilder {
	private String systemId;
	private Source source;
	private final List<Closeable> opened = new ArrayList<Closeable>();
	private Transformer transformer;
	private ErrorCatcher listener;
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	{
		builder.setNamespaceAware(true);
	}

	public TransformBuilder(Transformer transformer, String systemId,
			Source source, Closeable closeable, final URIResolver resolver) {
		this.transformer = transformer;
		this.systemId = systemId;
		listener = new ErrorCatcher(source.getSystemId());
		transformer.setErrorListener(listener);
		transformer.setURIResolver(new URIResolver() {
			public Source resolve(String href, String base) throws TransformerException {
				Source source = resolver.resolve(href, base);
				if (source instanceof StreamSource) {
					InputStream in = ((StreamSource)source).getInputStream();
					if (in != null) {
						synchronized (opened) {
							opened.add(in);
						}
					}
				}
				return source;
			}
		});
		this.source = source;
		if (closeable != null) {
			this.opened.add(closeable);
		}
	}

	protected void fatalError(TransformerException exception) {
		listener.fatalError(exception);
	}

	protected void ioException(IOException exception) {
		listener.ioException(exception);
	}

	public TransformBuilder with(String name, String value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, CharSequence value) {
		if (value != null) {
			transformer.setParameter(name, value.toString());
		}
		return this;
	}

	public TransformBuilder with(String name, Character value) {
		if (value != null) {
			transformer.setParameter(name, String.valueOf(value));
		}
		return this;
	}

	public TransformBuilder with(String name, char value) {
		transformer.setParameter(name, String.valueOf(value));
		return this;
	}

	public TransformBuilder with(String name, boolean value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, byte value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, short value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, int value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, long value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, float value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, double value) {
		transformer.setParameter(name, value);
		return this;
	}

	public TransformBuilder with(String name, Boolean value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Byte value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Short value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Integer value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Long value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Float value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Double value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, BigInteger value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, BigDecimal value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Document value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Element value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, DocumentFragment value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Node value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, NodeList value) {
		if (value != null) {
			transformer.setParameter(name, value);
		}
		return this;
	}

	public TransformBuilder with(String name, Readable value)
			throws TransformerException {
		if (value == null)
			return this;
		if (value instanceof Reader)
			return with(name, (Reader) value);
		return with(name, new ReadableReader(value));
	}

	public TransformBuilder with(String name, byte[] value)
			throws TransformerException {
		if (value == null)
			return this;
		return with(name, new ByteArrayInputStream(value));
	}

	public TransformBuilder with(String name, ReadableByteChannel value)
			throws TransformerException {
		if (value == null)
			return this;
		return with(name, Channels.newInputStream(value));
	}

	public TransformBuilder with(String name, ByteArrayOutputStream value)
			throws TransformerException {
		if (value == null)
			return this;
		return with(name, value.toByteArray());
	}

	public TransformBuilder with(final String name, final XMLEventReader value)
			throws TransformerException, XMLStreamException, IOException {
		if (value == null)
			return this;
		Source source;
		try {
			source = new StAXSource(value);
		} catch (NullPointerException e) {
			// no location
			PipedInputStream input = new PipedInputStream();
			final PipedOutputStream output = new PipedOutputStream(input);
			XSLTransformer.executor.execute(new Runnable() {
				public String toString() {
					return "parsing " + name;
				}

				public void run() {
					try {
						XMLEventWriter writer = factory
								.createXMLEventWriter(output);
						try {
							writer.add(value);
						} finally {
							writer.close();
							output.close();
						}
					} catch (XMLStreamException e) {
						fatalError(new TransformerException(e));
					} catch (IOException e) {
						ioException(e);
					}
				}
			});
			source = new StreamSource(input);
		}
		return with(name, source);
	}

	public TransformBuilder with(String name, InputStream value)
			throws TransformerException {
		if (value == null)
			return this;
		return with(name, new StreamSource(value));
	}

	public TransformBuilder with(String name, Reader value)
			throws TransformerException {
		if (value == null)
			return this;
		return with(name, new StreamSource(value));
	}

	public String asString() throws IOException {
		CharSequence seq = asCharSequence();
		if (seq == null)
			return null;
		return seq.toString();
	}

	public CharSequence asCharSequence() throws IOException {
		StringWriter output = new StringWriter();
		transform(new StreamResult(output), output);
		if (listener.isIOException())
			throw listener.getIOException();
		StringBuffer buffer = output.getBuffer();
		if (buffer.length() < 100 && isEmpty(buffer.toString()))
			return null;
		return buffer;
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

	public ByteArrayOutputStream asByteArrayOutputStream() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		transform(new StreamResult(output), output);
		if (listener.isIOException())
			throw listener.getIOException();
		if (output.size() < 200 && isEmpty(output.toByteArray(), output.size()))
			return null;
		return output;
	}

	public Object asObject() throws TransformerException, IOException,
			ParserConfigurationException {
		return asDocumentFragment();
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
		if (output.getNode().hasChildNodes())
			return frag;
		return null;
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
		if (output.getNode().hasChildNodes())
			return output.getNode();
		return null;
	}

	public XMLEventReader asXMLEventReader() throws IOException {
		XMLEventReaderFactory infactory = XMLEventReaderFactory.newInstance();
		try {
			Properties oformat = new Properties();
			oformat.put("method", "xml");
			transformer.setOutputProperties(oformat);
			return infactory.createXMLEventReader(asReader());
		} catch (XMLStreamException e) {
			throw asIOException(e);
		}
	}

	public InputStream asInputStream() throws IOException {
		final PipedOutputStream output = new PipedOutputStream();
		PipedInputStream input = new PipedInputStream(output) {
			public void close() throws IOException {
				try {
					if (listener.isIOException())
						throw new IOException(listener.getIOException());
				} finally {
					super.close();
				}
			}

			public String toString() {
				if (systemId == null)
					return transformer + " " + source.getSystemId();
				return systemId + " " + source.getSystemId();
			}
		};
		XSLTransformer.executor.execute(new Runnable() {
			public void run() {
				transform(new StreamResult(output), output);
			}
		});
		BufferedInputStream buffer = new BufferedInputStream(input);
		ByteBuffer buf = ByteBuffer.allocate(200);
		buffer.mark(buf.limit());
		while (buf.hasRemaining()) {
			int read = buffer.read(buf.array(), buf.position(), buf.remaining());
			if (read < 0)
				break;
			buf.position(buf.position() + read);
		}
		if (buf.hasRemaining() && isEmpty(buf.array(), buf.position())) {
			input.close();
			return null;
		}
		buffer.reset();
		return buffer;
	}

	public Reader asReader() throws IOException {
		final PipedWriter output = new PipedWriter();
		PipedReader input = new PipedReader(output) {
			public void close() throws IOException {
				try {
					if (listener.isIOException())
						throw new IOException(listener.getIOException());
				} finally {
					super.close();
				}
			}

			public String toString() {
				if (systemId == null)
					return transformer + " " + source.getSystemId();
				return systemId + " " + source.getSystemId();
			}
		};
		XSLTransformer.executor.execute(new Runnable() {
			public void run() {
				transform(new StreamResult(output), output);
			}
		});
		BufferedReader reader = new BufferedReader(input);
		CharBuffer cbuf = CharBuffer.allocate(100);
		reader.mark(cbuf.limit());
		while (cbuf.hasRemaining()) {
			int read = reader.read(cbuf);
			if (read < 0)
				break;
		}
		if (cbuf.hasRemaining() && isEmpty(cbuf.flip().toString())) {
			input.close();
			return null;
		}
		reader.reset();
		return reader;
	}

	private boolean isEmpty(byte[] buf, int len) {
		if (len == 0)
			return true;
		String xml = decodeXML(buf, len);
		if (xml == null)
			return false; // Don't start with < in UTF-8 or UTF-16
		return isEmpty(xml);
	}

	private boolean isEmpty(String xml) {
		if (xml == null || xml.length() < 1 || xml.trim().length() < 1)
			return true;
		if (xml.length() < 2)
			return false;
		if (xml.charAt(0) != '<' || xml.charAt(1) != '?')
			return false;
		if (xml.charAt(xml.length() - 2) != '?'
				|| xml.charAt(xml.length() - 1) != '>')
			return false;
		for (int i = 1, n = xml.length() - 2; i < n; i++) {
			if (xml.charAt(i) == '<')
				return false;
		}
		return true;
	}

	private TransformBuilder with(String name, Source source)
			throws TransformerException {
		DOMResult result = new DOMResult();
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.newTransformer().transform(source, result);
		Node node = result.getNode();
		return with(name, node);
	}

	private void transform(DOMResult output) throws TransformerException,
			IOException {
		transform(output, null);
		if (listener.isIOException())
			throw listener.getIOException();
	}

	private void transform(Result result, Closeable output) {
		try {
			if (output != null) {
				synchronized (opened) {
					opened.add(output);
				}
			}
			if (listener.isFatal())
				throw listener.getFatalError();
			transformer.transform(source, result);
			if (listener.isFatal())
				throw listener.getFatalError();
		} catch (TransformerException e) {
			listener.ioException(asIOException(e));
		} finally {
			synchronized (opened) {
				for (Closeable closeable : opened) {
					try {
						closeable.close();
					} catch (IOException e) {
						listener.ioException(e);
					}
				}
				opened.clear();
			}
		}
	}

	private IOException asIOException(Exception e) {
		if (e instanceof IOException)
			return (IOException) e;
		Throwable cause = e.getCause();
		while (cause != null) {
			if (cause instanceof IOException)
				return (IOException) cause;
			cause = cause.getCause();
		}
		return new IOException(e);
	}

	/**
	 * Decodes the stream just enough to read the &lt;?xml declaration. This
	 * method can distinguish between UTF-16, UTF-8, and EBCDIC xml files, but
	 * not UTF-32.
	 * 
	 * @return a string starting with &lt; or null
	 */
	private String decodeXML(byte[] buf, int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append((char) buf[i]);
		}
		String s = sb.toString();
		String APPFcharset = null; // 'charset' according to XML APP. F
		int byteOrderMark = 0;
		if (s.startsWith("\u00FE\u00FF")) {
			APPFcharset = "UTF-16BE";
			byteOrderMark = 2;
		} else if (s.startsWith("\u00FF\u00FE")) {
			APPFcharset = "UTF-16LE";
			byteOrderMark = 2;
		} else if (s.startsWith("\u00EF\u00BB\u00BF")) {
			APPFcharset = "UTF-8";
			byteOrderMark = 3;
		} else if (s.startsWith("\u0000<")) {
			APPFcharset = "UTF-16BE";
		} else if (s.startsWith("<\u0000")) {
			APPFcharset = "UTF-16LE";
		} else if (s.startsWith("<")) {
			APPFcharset = "US-ASCII";
		} else if (s.startsWith("\u004C\u006F\u00A7\u0094")) {
			APPFcharset = "CP037"; // EBCDIC
		} else {
			return null;
		}
		try {
			byte[] bytes = s.substring(byteOrderMark).getBytes("iso-8859-1");
			String xml = new String(bytes, APPFcharset);
			if (xml.startsWith("<"))
				return xml;
			return null;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
