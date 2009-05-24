package org.openrdf.server.metadata;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.WebResource;

public class ContentNegotiationTest extends MetadataServerTestCase {

	public static class Alternate {
		@rel("alternate")
		@operation("boolean")
		@type("application/sparql-results+xml")
		public boolean getBoolean() {
			return true;
		}

		@rel("alternate")
		@operation("rdf")
		@type("application/rdf+xml")
		public Model getModel() {
			return new LinkedHashModel();
		}

		@operation("my")
		public Model getMyModel() {
			return new LinkedHashModel();
		}

		@operation("my")
		public void setMyModel(Model model) {
		}

		@operation("my")
		public boolean getMyBoolean() {
			return true;
		}

		@operation("my")
		public void setMyBoolean(boolean bool) {
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Alternate.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testAlternate() throws Exception {
		WebResource web = client.path("/");
		web.accept("application/rdf+xml").get(Model.class);
		web.accept("application/sparql-results+xml").get(String.class);
	}

	public void testGetOperation() throws Exception {
		WebResource web = client.path("/").queryParam("my", "");
		web.accept("application/rdf+xml").get(Model.class);
		web.accept("application/sparql-results+xml").get(String.class);
	}

	public void testPutOperation() throws Exception {
		WebResource web = client.path("/").queryParam("my", "");
		web.type("application/rdf+xml").put(new LinkedHashModel());
		String str = web.accept("application/sparql-results+xml").get(String.class);
		web.type("application/sparql-results+xml").put(str);
	}
}