package org.openrdf.http.object;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.AliasSupport;
import org.openrdf.http.object.behaviours.DescribeSupport;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.http.object.behaviours.TextFile;
import org.openrdf.http.object.concepts.Alias;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class DataResourceTest extends MetadataServerTestCase {

	public static abstract class WorldFile implements HTTPFileObject {
		@method("GET")
		@type("text/world")
		public InputStream getInputStream() throws IOException {
			return openInputStream();
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(TextFile.class, "urn:mimetype:text/plain");
		config.addBehaviour(WorldFile.class, "urn:mimetype:text/world");
		config.addBehaviour(PUTSupport.class);
		config.addConcept(Alias.class);
		config.addBehaviour(AliasSupport.class);
		config.addBehaviour(DescribeSupport.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testPUT() throws Exception {
		client.path("hello").put("world");
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testPUTRoot() throws Exception {
		client.put("world");
		assertEquals("world", client.get(String.class));
	}

	public void testRedirect() throws Exception {
		client.path("world").put("world");
		client.path("hello").header("Content-Location", client.path("world").getURI()).put();
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testRelativeRedirect() throws Exception {
		client.path("world").put("world");
		client.path("hello").header("Content-Location", "world").put();
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testDELETE() throws Exception {
		client.path("hello").put("world");
		client.path("hello").delete();
		try {
			client.path("hello").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testPUTIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put("world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put("new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).put("bad world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testDELETEIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put("world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put("new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testPUTContentType() throws Exception {
		WebResource hello = client.path("hello.txt");
		hello.type("text/world").put("world");
		assertEquals("text/world", hello.head().getMetadata().getFirst("Content-Type"));
	}

	public void testNoOptions() throws Exception {
		ClientResponse options = client.path("hello").options(ClientResponse.class);
		String allows = options.getMetadata().getFirst("Allow");
		assertEquals("OPTIONS, TRACE, PUT", allows);
	}

	public void testOPTIONS() throws Exception {
		client.path("hello").put("world");
		ClientResponse options = client.path("hello").options(ClientResponse.class);
		String allows = options.getMetadata().getFirst("Allow");
		assertEquals("OPTIONS, TRACE, GET, HEAD, PUT, DELETE", allows);
	}
}
