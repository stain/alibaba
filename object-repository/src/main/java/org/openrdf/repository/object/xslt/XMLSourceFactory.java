package org.openrdf.repository.object.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

public class XMLSourceFactory {
	public static XMLSourceFactory newInstance() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(IGNORE_EXTERNAL_DTD, true);
		return new XMLSourceFactory(factory);
	}

	private static final String IGNORE_EXTERNAL_DTD = "http://java.sun.com/xml/stream/properties/ignore-external-dtd";
	private XMLInputFactory factory;

	protected XMLSourceFactory(XMLInputFactory inFactory) {
		this.factory = inFactory;
	}

	public void close(Source source) throws TransformerException {
		try {
			if (source instanceof StreamSource) {
				StreamSource ss = (StreamSource) source;
				InputStream in = ss.getInputStream();
				if (in == null) {
					ss.getReader().close();
				} else {
					ss.getInputStream().close();
				}
			}
			if (source instanceof StAXSource) {
				final StAXSource stax = (StAXSource) source;
				XMLEventReader er = stax.getXMLEventReader();
				if (er == null) {
					stax.getXMLStreamReader().close();
				} else {
					stax.getXMLEventReader().close();
				}
			}
		} catch (IOException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(String systemId) throws TransformerException {
		return new StreamSource(systemId);
	}

	public Source createSource(InputStream in) throws TransformerException {
		try {
			return createSource(factory.createXMLStreamReader(in));
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(InputStream in, String systemId)
			throws TransformerException {
		try {
			return createSource(factory.createXMLStreamReader(systemId, in));
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(Reader reader) throws TransformerException {
		try {
			return createSource(factory.createXMLStreamReader(reader));
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(Reader reader, String systemId)
			throws TransformerException {
		try {
			return createSource(factory.createXMLStreamReader(systemId, reader));
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		}
	}

	private Source createSource(XMLStreamReader reader) {
		return new StAXSource(reader);
	}

}
