package org.openrdf.repository.object.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ReaderTransform extends TransformBuilder {
	private final Reader source;
	private final String systemId;
	private final XMLEventReaderFactory inFactory = XMLEventReaderFactory
			.newInstance();
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();

	public ReaderTransform(Reader source, String systemId) {
		this.source = source;
		this.systemId = systemId;
	}

	public void close() throws TransformerException {
		try {
			source.close();
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		}
	}

	@Override
	public Document asDocument() throws TransformerException {
		Reader reader = asReader();
		try {
			try {
				return builder.parse(reader, systemId);
			} catch (SAXException e) {
				throw handle(new TransformerException(e));
			} catch (ParserConfigurationException e) {
				throw handle(new TransformerException(e));
			} finally {
				reader.close();
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
		try {
			return inFactory.createXMLEventReader(source);
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
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
		return source;
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
	protected void setParameter(String name, Object value) {
		// no parameters
	}

}
