package org.openrdf.repository.object.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLEventTransform extends TransformBuilder {
	private XMLEventReader source;
	private final String systemId;
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();

	public XMLEventTransform(XMLEventReader source, String systemId) {
		this.source = source;
		this.systemId = systemId;
	}

	public void close() throws TransformerException {
		try {
			source.close();
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		}
	}

	@Override
	public Document asDocument() throws TransformerException {
		InputStream in = asInputStream();
		try {
			try {
				return builder.parse(in, systemId);
			} catch (SAXException e) {
				throw handle(new TransformerException(e));
			} catch (ParserConfigurationException e) {
				throw handle(new TransformerException(e));
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public XMLEventReader asXMLEventReader() throws TransformerException {
		return source;
	}

	@Override
	public InputStream asInputStream() throws TransformerException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
			try {
				toOutputStream(baos);
			} finally {
				baos.close();
			}
			return new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public Reader asReader() throws TransformerException {
		try {
			CharArrayWriter caw = new CharArrayWriter(8192);
			try {
				toWriter(caw);
			} catch (IOException e) {
				throw handle(new TransformerException(e));
			} finally {
				caw.close();
			}
			return new CharArrayReader(caw.toCharArray());
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public void toOutputStream(OutputStream out) throws IOException,
			TransformerException {
		XMLEventReader reader = asXMLEventReader();
		try {
			try {
				XMLEventWriter writer = outFactory.createXMLEventWriter(out,
						"UTF-8");
				writer.add(reader);
				writer.flush();
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public void toWriter(Writer writer) throws IOException,
			TransformerException {
		XMLEventReader reader = asXMLEventReader();
		try {
			try {
				XMLEventWriter xml = outFactory.createXMLEventWriter(writer);
				xml.add(reader);
				xml.flush();
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	protected void setParameter(String name, Object value) {
		// no parameters
	}

}
