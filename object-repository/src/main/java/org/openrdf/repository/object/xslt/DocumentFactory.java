package org.openrdf.repository.object.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentFactory {
	private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	private static final Logger logger = LoggerFactory
			.getLogger(DocumentFactory.class);

	public static DocumentFactory newInstance() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setIgnoringComments(false);
		factory.setIgnoringElementContentWhitespace(false);
		try {
			factory.setFeature(LOAD_EXTERNAL_DTD, false);
		} catch (ParserConfigurationException e) {
			logger.warn(e.toString(), e);
		}
		return new DocumentFactory(factory);
	}

	private final DocumentBuilderFactory factory;

	protected DocumentFactory(DocumentBuilderFactory builder) {
		this.factory = builder;
	}

	public Document newDocument() throws ParserConfigurationException {
		return factory.newDocumentBuilder().newDocument();
	}

	public Document parse(InputStream in, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(in, systemId);
	}

	public Document parse(InputStream in) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(in);
	}

	public Document parse(String url) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(url);
	}

	public Document parse(InputSource is) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		InputSource is = new InputSource(reader);
		is.setSystemId(systemId);
		return factory.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader) throws SAXException,
			IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(new InputSource(reader));
	}

}
