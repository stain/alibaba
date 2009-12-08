package org.openrdf.http.object.providers;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.repository.object.annotations.matches;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class DOMProviderTest extends MetadataServerTestCase {

	private static final String XML_NO = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	@matches("/")
	public static class Controller {
		private DocumentBuilderFactory builder;

		public Controller() {
			builder = DocumentBuilderFactory.newInstance();
			builder.setNamespaceAware(true);
		}

		@operation("document")
		@type("application/xml")
		public Document document() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			Element element = doc.createElement("document");
			doc.appendChild(element);
			return doc;
		}

		@operation("element")
		@type("application/xml")
		public Element element() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			return doc.createElement("element");
		}

		@operation("fragment")
		@type("application/xml")
		public DocumentFragment fragment() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element element = doc.createElement("fragment");
			frag.appendChild(element);
			return frag;
		}

		@operation("fragment-dual")
		@type("application/xml")
		public DocumentFragment fragmentDual() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element first = doc.createElement("first");
			Element second = doc.createElement("second");
			frag.appendChild(first);
			frag.appendChild(second);
			return frag;
		}

		@operation("fragment-whitespace")
		@type("application/xml")
		public DocumentFragment fragmentWhite() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Text space = doc.createTextNode(" ");
			Element element = doc.createElement("fragment");
			frag.appendChild(space);
			frag.appendChild(element);
			return frag;
		}

		@operation("document")
		@type("application/xml")
		public void document(Document document) throws ParserConfigurationException {
			assert document.hasChildNodes();
		}

		@operation("element")
		@type("application/xml")
		public void element(Element element) throws ParserConfigurationException {
			assert element.getNodeName().equals("element");
		}

		@operation("fragment")
		@type("application/xml")
		public void fragment(DocumentFragment frag) throws ParserConfigurationException {
			assert frag.hasChildNodes();
		}

		@operation("fragment-dual")
		@type("application/xml")
		public void fragmentDual(DocumentFragment frag) throws ParserConfigurationException {
			assertEquals(2, frag.getChildNodes().getLength());
		}

		@operation("fragment-whitespace")
		@type("application/xml")
		public void fragmentWhite(DocumentFragment frag) throws ParserConfigurationException {
			assertEquals(2, frag.getChildNodes().getLength());
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Controller.class);
		super.setUp();
	}

	public void testDocument() throws Exception {
		assertEquals(XML_NO + "<document/>", getString("document"));
		putString("document", "<document/>");
	}

	public void testElement() throws Exception {
		assertEquals(XML + "<element/>", getString("element"));
		putString("element", "<element/>");
	}

	public void testFragment() throws Exception {
		assertEquals("<fragment/>", getString("fragment"));
		putString("fragment", "<fragment/>");
	}

	public void testFragmentDual() throws Exception {
		assertEquals("<first/><second/>", getString("fragment-dual"));
		putString("fragment-dual", "<first/><second/>");
	}

	public void testFragmentWhitespace() throws Exception {
		assertEquals(" <fragment/>", getString("fragment-whitespace"));
		putString("fragment-whitespace", " <fragment/>");
	}

	private String getString(String operation) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		return req.get(String.class);
	}

	private void putString(String operation, String data) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		req.put(data);
	}

}