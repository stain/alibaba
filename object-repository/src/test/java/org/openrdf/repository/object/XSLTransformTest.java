package org.openrdf.repository.object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;

import junit.framework.Test;

import org.openrdf.annotations.iri;
import org.openrdf.annotations.name;
import org.openrdf.repository.object.base.ObjectRepositoryTestCase;
import org.openrdf.repository.object.base.RepositoryTestCase;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XSLTransformTest extends ObjectRepositoryTestCase {
	private static String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final String XML_STRING = XML + "<AliBaba/>";
	private static final byte[] XML_BYTES = XML_STRING.getBytes(Charset
			.forName("UTF-8"));
	public static final String XSLT_ECHO = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
			+ "<xsl:template match='/'>"
			+ "<xsl:copy-of select='.'/>"
			+ "</xsl:template></xsl:stylesheet>";
	public static final String XSLT_HELLO_WORLD = "<message xsl:version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>hello world!</message>";

	private Concept concept;

	public static Test suite() throws Exception {
		return RepositoryTestCase.suite(XSLTransformTest.class);
	}

	@iri(MSG.NAMESPACE + "xslt")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface xslt {
		String value();
	}

	@iri("urn:mimetype:application/xml")
	public interface XMLFile {

	}

	@iri("urn:test:Concept")
	public interface Concept {

		@xslt(XSLT_ECHO)
		String echo(String input);

		@xslt(XSLT_ECHO)
		Node echo(Node input);

		@xslt(XSLT_ECHO)
		Document echo(Document input);

		@xslt(XSLT_ECHO)
		DocumentFragment echo(DocumentFragment input);

		@xslt(XSLT_ECHO)
		Element echo(Element input);

		@xslt(XSLT_ECHO)
		XMLEventReader echo(XMLEventReader input);

		@xslt(XSLT_ECHO)
		Readable echo(Readable input);

		@xslt(XSLT_ECHO)
		Reader echo(Reader input);

		@xslt(XSLT_ECHO)
		ReadableByteChannel echo(ReadableByteChannel input);

		@xslt(XSLT_ECHO)
		InputStream echo(InputStream input);

		@xslt(XSLT_ECHO)
		ByteArrayOutputStream echo(ByteArrayOutputStream input);

		@xslt(XSLT_ECHO)
		byte[] echo(byte[] input);

		@xslt(XSLT_ECHO)
		String toString(String input);

		@xslt(XSLT_ECHO)
		String toString(Node input);

		@xslt(XSLT_ECHO)
		String toString(Document input);

		@xslt(XSLT_ECHO)
		String toString(DocumentFragment input);

		@xslt(XSLT_ECHO)
		String toString(Element input);

		@xslt(XSLT_ECHO)
		String toString(XMLEventReader input);

		@xslt(XSLT_ECHO)
		String toString(Readable input);

		@xslt(XSLT_ECHO)
		String toString(Reader input);

		@xslt(XSLT_ECHO)
		String toString(ReadableByteChannel input);

		@xslt(XSLT_ECHO)
		String toString(InputStream input);

		@xslt(XSLT_ECHO)
		String toString(ByteArrayOutputStream input);

		@xslt(XSLT_ECHO)
		String toString(byte[] input);

		@xslt(XSLT_ECHO)
		String echo(XMLFile input);

		@xslt(XSLT_ECHO)
		String echo(File input);

		@xslt(XSLT_ECHO)
		String echo(URL input);

		@xslt(XSLT_HELLO_WORLD)
		Element hello();

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:param name='arg' />"
				+ "<xsl:output method='text' />"
				+ "<xsl:template match='/'>"
				+ "<xsl:text>hello </xsl:text><xsl:value-of select='$arg'/><xsl:text>!</xsl:text>"
				+ "</xsl:template></xsl:stylesheet>")
		String hello(@name("arg") String arg);

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:template match='/'>"
				+ "</xsl:template></xsl:stylesheet>")
		InputStream nothing();
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Concept.class);
		config.addConcept(XMLFile.class);
		super.setUp();
		concept = con.addDesignation(con.getObject("urn:test:concept"),
				Concept.class);
	}

	public void testString() throws Exception {
		assertEquals(XML_STRING, concept.echo("<AliBaba/>"));
	}

	public void testNode() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo((Node) doc)));
	}

	public void testDocument() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo(doc)));
	}

	public void testDocumentFragment() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo(frag)));
	}

	public void testElement() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element element = doc.createElement("AliBaba");
		assertEquals(XML_STRING, concept.toString(concept.echo(element)));
	}

	public void testXMLEventReader() throws Exception {
		Reader reader = new StringReader(XML_STRING);
		XMLEventReader events = XMLEventReaderFactory.newInstance()
				.createXMLEventReader("urn:test:location", reader);
		XMLEventReader result = concept.echo(events);
		assertEquals(XML_STRING, concept.toString(result));
	}

	public void testReadable() throws Exception {
		Readable reader = new StringReader(XML_STRING);
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testReader() throws Exception {
		Reader reader = new StringReader(XML_STRING);
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testReadableByteChannel() throws Exception {
		ReadableByteChannel reader = Channels
				.newChannel(new ByteArrayInputStream(XML_BYTES));
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testInputStream() throws Exception {
		InputStream stream = new ByteArrayInputStream(XML_BYTES);
		assertEquals(XML_STRING, concept.toString(concept.echo(stream)));
	}

	public void testByteArrayOutputStream() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(XML_BYTES);
		assertEquals(XML_STRING, concept.toString(concept.echo(stream)));
	}

	public void testByteArray() throws Exception {
		assertEquals(XML_STRING, concept.toString(concept.echo(XML_BYTES)));
	}

	public void testFile() throws Exception {
		File tmp = File.createTempFile("transform", "xslt");
		try {
			FileOutputStream out = new FileOutputStream(tmp);
			try {
				out.write(XML_BYTES);
			} finally {
				out.close();
			}
			assertEquals(XML_STRING, concept.echo(tmp));
		} finally {
			tmp.delete();
		}
	}

	public void testURL() throws Exception {
		File tmp = File.createTempFile("transform", "xslt");
		try {
			FileOutputStream out = new FileOutputStream(tmp);
			try {
				out.write(XML_BYTES);
			} finally {
				out.close();
			}
			assertEquals(XML_STRING, concept.echo(tmp.toURI().toURL()));
		} finally {
			tmp.delete();
		}
	}

	public void testConcept() throws Exception {
		File tmp = File.createTempFile("transform", "xslt");
		try {
			FileOutputStream out = new FileOutputStream(tmp);
			try {
				out.write(XML_BYTES);
			} finally {
				out.close();
			}
			String uri = tmp.toURI().toASCIIString();
			XMLFile file = con
					.addDesignation(con.getObject(uri), XMLFile.class);
			assertEquals(XML_STRING, concept.echo(file));
		} finally {
			tmp.delete();
		}
	}

	public void testNoInput() throws Exception {
		assertEquals("hello world!", concept.hello().getTextContent());
	}

	public void testParameter() throws Exception {
		assertEquals("hello world!", concept.hello("world"));
	}

	public void testNothing() throws Exception {
		InputStream in = concept.nothing();
		if (in != null) {
			System.out.print("Open>");
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				System.out.write(buf, 0, read);
			}
			System.out.println("<Close");
		}
		assertNull(in);
	}

}
