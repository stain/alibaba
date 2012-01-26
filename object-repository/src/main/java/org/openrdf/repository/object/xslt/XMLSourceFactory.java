package org.openrdf.repository.object.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLSourceFactory {
	public static XMLSourceFactory newInstance() {
		// StreamSource loads external DTD
		// XML Stream/Event to StAXSource drops comments
		return new XMLSourceFactory(DocumentFactory.newInstance());
	}

	private DocumentFactory factory;

	protected XMLSourceFactory(DocumentFactory factory) {
		this.factory = factory;
	}

	public void close(Source source) throws TransformerException {
		try {
			if (source instanceof StreamSource) {
				StreamSource ss = (StreamSource) source;
				if (ss.getReader() != null) {
					ss.getReader().close();
				}
				if (ss.getInputStream() != null) {
					ss.getInputStream().close();
				}
			}
			if (source instanceof StAXSource) {
				final StAXSource stax = (StAXSource) source;
				if (stax.getXMLEventReader() != null) {
					stax.getXMLEventReader().close();
				}
				if (stax.getXMLStreamReader() != null) {
					stax.getXMLStreamReader().close();
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

	public Source createSource(InputStream in, String systemId)
			throws TransformerException {
		try {
			try {
				return createSource(factory.parse(in, systemId), systemId);
			} finally {
				in.close();
			}
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (IOException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(Reader reader, String systemId)
			throws TransformerException {
		try {
			try {
				return createSource(factory.parse(reader, systemId), systemId);
			} finally {
				reader.close();
			}
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (IOException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		}
	}

	public Source createSource(Document document, String systemId) {
		return new DOMSource(document, systemId);
	}

	public Source createSource(Node node, String systemId) {
		return new DOMSource(node, systemId);
	}

}
