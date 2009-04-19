package org.openrdf.server.metadata.providers;

import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.server.metadata.annotations.purpose;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RDFObjectProviderTest extends MetadataServerTestCase {

	@rdf("urn:test:Document")
	public interface Document {
		@purpose("author")
		@rdf("urn:test:author")
		Person getAuthor();
		@purpose("author")
		void setAuthor(Person author);
	}

	@rdf("urn:test:Person")
	public interface Person {
		@rdf("urn:test:name")
		String getName();
		void setName(String name);
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Document.class);
		config.addConcept(Person.class);
		super.setUp();
	}

	public void testNamedAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAnonyoumsAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObjectFactory().createObject(), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class).setAuthor(author);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingNamedAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", base+"/auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingRelativeAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		Person author = con.addType(con.getObject(base+"/auth"), Person.class);
		author.setName("James");
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		web.header("Content-Location", "auth").put();
		Model model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}

	public void testAddingAnonyoumsAuthor() throws Exception {
		ObjectConnection con = repository.getConnection();
		con.addType(con.getObject(base+"/doc"), Document.class);
		con.close();
		WebResource web = client.path("/doc").queryParam("author", "");
		try {
			web.accept("application/rdf+xml").get(Model.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
		Model model = new LinkedHashModel();
		BNode auth = vf.createBNode();
		model.add(auth, RDF.TYPE, vf.createURI("urn:test:Person"));
		model.add(auth, vf.createURI("urn:test:name"), vf.createLiteral("James"));
		web.type("application/rdf+xml").put(model);
		model = web.accept("application/rdf+xml").get(Model.class);
		assertTrue(model.contains(null, vf.createURI("urn:test:name"), vf.createLiteral("James")));
	}
}
