package org.openrdf.http.object;

import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.transform;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.matches;
import org.openrdf.repository.object.annotations.xslt;

import com.sun.jersey.api.client.WebResource;

public class TransformTest extends MetadataServerTestCase {
	private static final String TURTLE_HELLO = "\n<urn:test:hello> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> \"hello world!\" .\n";
	public static final String XSLT_EXECUTE = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
			+ "<xsl:output omit-xml-declaration='yes'/>"
			+ "<xsl:template match='echo'>"
			+ "<xsl:copy-of select='node()'/>"
			+ "</xsl:template></xsl:stylesheet>";

	@matches("/service")
	public static abstract class Service {
		@operation("world")
		@type("text/xml")
		@transform("urn:test:execute")
		public String world() {
			return "<echo>hello world!</echo>";
		}

		@operation("hello")
		@type("text/plain")
		public String hello(@transform("urn:test:execute") String world) {
			return "hello " + world + "!";
		}

		@type("text/plain")
		@iri("urn:test:execute")
		@xslt(XSLT_EXECUTE)
		public abstract String execute(String xml);

		@operation("turtle")
		@type("application/x-turtle")
		public Model turtle(
				@transform("urn:test:rdfvalue") GraphQueryResult result)
				throws QueryEvaluationException {
			Model model = new LinkedHashModel();
			while (result.hasNext()) {
				model.add(result.next());
			}
			return model;
		}

		@type("application/rdf+xml")
		@iri("urn:test:rdfvalue")
		public Model extract(String input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl(input));
			return model;
		}

		@operation("increment")
		public int increment(
				@transform("urn:test:decrypt") @parameter("number") int base) {
			return base + 1;
		}

		@iri("urn:test:decrypt")
		public int decrypt(String number) {
			return Integer.parseInt(number, 2);
		}

		@operation("indirect")
		@type("application/x-turtle")
		@transform("urn:test:serialize")
		public Model indirect() {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl("hello world!"));
			return model;
		}

		@transform("urn:test:execute")
		@type("text/xml")
		@iri("urn:test:serialize")
		public String serialize(Model model) {
			return "<echo>" + model.objectString() + "</echo>";
		}

		@operation("parse")
		@type("text/plain")
		public String parse(
				@transform("urn:test:rdfvalue") GraphQueryResult result)
				throws QueryEvaluationException {
			return result.next().getObject().stringValue();
		}

		@type("text/plain")
		@iri("urn:test:parse")
		public String parseModel(@transform("urn:test:read") Model model)
				throws QueryEvaluationException {
			return model.objectString();
		}

		@type("application/x-turtle")
		@iri("urn:test:read")
		public Model read(Model input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl("hello " + input.objectString() + "!"));
			return model;
		}

		@operation("post")
		@type("text/plain")
		@transform("urn:test:parse")
		public Model post(String input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl(input));
			return model;
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Service.class);
		super.setUp();
	}

	public void testNoOutboundTransform() {
		WebResource service = client.path("service").queryParam("world", "");
		assertEquals("<echo>hello world!</echo>", service.accept("text/xml")
				.get(String.class));
	}

	public void testOutboundTransform() {
		WebResource service = client.path("service").queryParam("world", "");
		assertEquals("hello world!", service.accept("text/plain").get(
				String.class));
	}

	public void testInboundTransform() {
		WebResource service = client.path("service").queryParam("hello", "");
		assertEquals("hello James!", service.accept("text/plain").post(
				String.class, "<echo>James</echo>"));
	}

	public void testRDFNoInboundTransform() {
		WebResource service = client.path("service").queryParam("turtle", "");
		assertEquals(TURTLE_HELLO, service.accept("application/x-turtle").type(
				"application/x-turtle").post(String.class, TURTLE_HELLO));
	}

	public void testRDFInboundTransform() {
		WebResource service = client.path("service").queryParam("turtle", "");
		assertEquals(TURTLE_HELLO, service.accept("application/x-turtle").type(
				"text/string").post(String.class, "hello world!"));
	}

	public void testTransformParameter() {
		WebResource service = client.path("service")
				.queryParam("increment", "").queryParam("number",
						Integer.toString(14, 2));
		assertEquals("15", service.type("text/plain").get(String.class));
	}

	public void testNoIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertEquals(TURTLE_HELLO, service.accept("application/x-turtle").get(
				String.class));
	}

	public void testPartialIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertEquals("<echo>hello world!</echo>", service.accept("text/xml").get(
				String.class));
	}

	public void testIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertEquals("hello world!", service.accept("text/plain").get(
				String.class));
	}

	public void testParseNoInboundTransform() {
		WebResource service = client.path("service").queryParam("parse", "");
		assertEquals("hello world!", service.accept("text/plain").type(
				"application/x-turtle").post(String.class, TURTLE_HELLO));
	}

	public void testParseInboundTransform() {
		WebResource service = client.path("service").queryParam("parse", "");
		assertEquals("hello world!", service.accept("text/plain").type(
				"text/string").post(String.class, "hello world!"));
	}

	public void testParseOutboundTransform() {
		WebResource service = client.path("service").queryParam("post", "");
		assertEquals("hello world!", service.accept("text/plain").type(
				"text/string").post(String.class, "world"));
	}
}
