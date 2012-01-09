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
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		builder.setNamespaceAware(true);
		try {
			builder.setFeature(LOAD_EXTERNAL_DTD, false);
		} catch (ParserConfigurationException e) {
			logger.warn(e.toString(), e);
		}
		return new DocumentFactory(builder);
	}

	private final DocumentBuilderFactory builder;

	protected DocumentFactory(DocumentBuilderFactory builder) {
		this.builder = builder;
	}

	public Document newDocument() throws ParserConfigurationException {
		return builder.newDocumentBuilder().newDocument();
	}

	public Document parse(InputStream in, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		return builder.newDocumentBuilder().parse(in, systemId);
	}

	public Document parse(InputStream in) throws SAXException, IOException,
			ParserConfigurationException {
		return builder.newDocumentBuilder().parse(in);
	}

	public Document parse(String url) throws SAXException, IOException,
			ParserConfigurationException {
		return builder.newDocumentBuilder().parse(url);
	}

	public Document parse(InputSource is) throws SAXException, IOException,
			ParserConfigurationException {
		return builder.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		InputSource is = new InputSource(reader);
		is.setSystemId(systemId);
		return builder.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader) throws SAXException,
			IOException, ParserConfigurationException {
		return builder.newDocumentBuilder().parse(new InputSource(reader));
	}

}
