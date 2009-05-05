package org.openrdf.repository.object;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Test;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.base.RepositoryTestCase;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ObjectConversionException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XmlLiteralTest extends RepositoryTestCase {

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(XmlLiteralTest.class);
	}

	public static class XmlLiteral {
		private static DocumentBuilder builder;

		private static Transformer transformer;

		private String xml;

		public XmlLiteral(String xml) {
			this.xml = xml;
		}

		public XmlLiteral(Document document) {
			this.xml = serialize(document);
		}

		public Document getDocument() {
			return deserialize(xml);
		}

		@Override
		public int hashCode() {
			return xml.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final XmlLiteral other = (XmlLiteral) obj;
			return xml.equals(other.xml);
		}

		@Override
		public String toString() {
			return xml;
		}

		private String serialize(Document object) {
			Source source = new DOMSource(object);
			CharArrayWriter writer = new CharArrayWriter();
			Result result = new StreamResult(writer);
			try {
				if (transformer == null) {
					transformer = TransformerFactory.newInstance()
							.newTransformer();
				}
				transformer.transform(source, result);
			} catch (Exception e) {
				throw new ObjectConversionException(e);
			}
			return writer.toString();
		}

		private Document deserialize(String xml) {
			try {
				char[] charArray = xml.toCharArray();
				CharArrayReader reader = new CharArrayReader(charArray);
				try {
					if (builder == null) {
						builder = DocumentBuilderFactory.newInstance()
								.newDocumentBuilder();
					}
					return builder.parse(new InputSource(reader));
				} finally {
					reader.close();
				}
			} catch (Exception e) {
				throw new ObjectConversionException(e);
			}
		}

	}

	@rdf("urn:test:Thing")
	public interface Thing {
		@rdf("urn:test:object")
		Object getObject();

		void setObject(Object value);
	}

	protected ObjectConnection manager;
	private ObjectRepositoryConfig module;

	public void testRoundTrip() throws Exception {
		Thing thing = manager.addDesignation(manager.getObjectFactory().createObject(), Thing.class);
		thing.setObject(new XmlLiteral("<b>object</b>"));
		assertTrue(thing.getObject() instanceof XmlLiteral);
		assertEquals(new XmlLiteral("<b>object</b>"), thing.getObject());
	}

	public void testRead() throws Exception {
		ObjectConnection con;
		con = manager;
		ValueFactory vf = con.getValueFactory();
		Thing thing = manager.addDesignation(manager.getObjectFactory().createObject(), Thing.class);
		Resource subj = (Resource) manager.addObject(thing);
		URI pred = vf.createURI("urn:test:object");
		Value obj = vf.createLiteral("<b>object</b>", RDF.XMLLITERAL);
		con.add(subj, pred, obj);
		assertTrue(thing.getObject() instanceof XmlLiteral);
		assertEquals(new XmlLiteral("<b>object</b>"), thing.getObject());
	}

	@Override
	protected void setUp() throws Exception {
		module = new ObjectRepositoryConfig();
		module.addDatatype(XmlLiteral.class, RDF.XMLLITERAL);
		module.addConcept(Thing.class);
		super.setUp();
		ObjectRepository managerFactory;
		managerFactory = (ObjectRepository) repository;
		manager = managerFactory.getConnection();
	}

	@Override
	protected Repository getRepository() throws Exception {
		return new ObjectRepositoryFactory().createRepository(module, super.getRepository());
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (manager.isOpen()) {
				manager.close();
			}
			super.tearDown();
		} catch (Exception e) {
		}
	}

}
