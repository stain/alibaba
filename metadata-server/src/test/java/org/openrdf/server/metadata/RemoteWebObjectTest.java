package org.openrdf.server.metadata;

import java.io.File;

import javax.ws.rs.core.MultivaluedMap;

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.annotations.header;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;
import org.openrdf.server.metadata.concepts.InternalWebObject;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class RemoteWebObjectTest extends MetadataServerTestCase {

	private ObjectConnection con;

	@iri("urn:test:WebInterface")
	public interface WebInterface {
		String getWorld();

		void setWorld(String world);

		@method("GET")
		String hello();

		@operation("post")
		String postPlain(@type("text/plain") String plain);

		@operation("post")
		String postXML(@type("text/xml") String xml);

		@operation("head")
		String head(@header("Content-Type") String type, String in,
				@header("X-Forward") String forward);
	}

	public static abstract class WebInterfaceSupport implements WebInterface {
		private String world = "World";

		public String getWorld() {
			return world;
		}

		public void setWorld(String world) {
			this.world = world;
		}

		public String hello() {
			return "Hello " + world + "!";
		}

		public String postPlain(String plain) {
			return "plain";
		}

		public String postXML(String xml) {
			return "xml";
		}

		public String head(String type, String in, String forward) {
			if (forward == null)
				return type;
			return null;
		}
	}

	@iri("urn:test:Chocolate")
	public static abstract class Chocolate implements WebObject {
		@method("DELETE")
		public void consume() throws RepositoryException {
			getObjectConnection().removeDesignation(this, Chocolate.class);
			delete();
		}

		@operation("mix")
		public HotChocolate mix(Milk milk) throws RepositoryException {
			ObjectConnection con = getObjectConnection();
			HotChocolate hot = con.addDesignation(this, HotChocolate.class);
			hot.add(milk);
			return hot;
		}
	}

	@iri("urn:test:Milk")
	public static class Milk {
		@operation("pour")
		public void pourInto(HotChocolate drink) {
			drink.add(this);
		}
	}

	@iri("urn:test:HotChocolate")
	public static abstract class HotChocolate extends Chocolate {
		public static int count;
		@iri("urn:test:amountOfMilk")
		private int milk;

		@operation("milk")
		public int getAmountOfMilk() {
			return milk;
		}

		public void add(Milk milk) {
			count++;
			this.milk++;
		}

		@method("DELETE")
		public void consume() throws RepositoryException {
			getObjectConnection().removeDesignation(this, HotChocolate.class);
			super.consume();
		}

	}

	public void setUp() throws Exception {
		config.addConcept(WebInterface.class);
		config.addBehaviour(WebInterfaceSupport.class);
		config.addConcept(Chocolate.class);
		config.addConcept(Milk.class);
		config.addConcept(HotChocolate.class);
		super.setUp();
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testLocal() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		File file = File.createTempFile("obj", "tmp");
		file.delete();
		((InternalWebObject) obj).initFileObject(file, true);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto");
		assertEquals("Hello Toronto!", obj.hello());
	}

	public void testRemote() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto"); // local in-memory property
		assertEquals("Hello World!", obj.hello());
	}

	public void testBodyType() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("plain", obj.postPlain("plain text"));
		assertEquals("xml", obj.postXML("xml text"));
	}

	public void testHeaders() throws Exception {
		String uri = client.path("/object").toString();
		con.addDesignation(con.getObject(uri), WebInterface.class);
		WebResource req = client.path("/object").queryParam("head", "");
		assertEquals("text/plain", req.type("text/plain").post(String.class, "txt"));
		MultivaluedMap<String, String> md;
		md = req.type("text/plain").post(ClientResponse.class, "txt").getMetadata();
		assertTrue(md.get("Vary").toString().contains("X-Forward"));
		md = req.options(ClientResponse.class).getMetadata();
		assertTrue(md.get("Access-Control-Allow-Headers").toString().contains("X-Forward"));
	}

	public void testRemoteHeaders() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("text/txt", obj.head("text/txt", "txt", null));
	}

	public void testGET() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		chocolate.add(milk);
		assertEquals(1, chocolate.getAmountOfMilk());
	}

	public void testPUT() throws Exception {
		HotChocolate.count = 0;
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		milk.pourInto(chocolate);
		assertEquals(1, HotChocolate.count);
		assertEquals(1, chocolate.getAmountOfMilk());
	}

	public void testDELETE() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		milk.pourInto(chocolate);
		chocolate.consume();
		try {
			chocolate.getAmountOfMilk();
			fail();
		} catch (MethodNotAllowed e) {
			// chocolate has already been eaten in another transaction
		}
	}

	public void testPOST() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		Chocolate chocolate = con.addDesignation(con.getObject(uri1),
				Chocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		HotChocolate hot = chocolate.mix(milk);
		assertEquals(1, hot.getAmountOfMilk());
	}
}
