package org.openrdf.server.metadata;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class DataResourceTest extends MetadataServerTestCase {

	public void testGET() throws Exception {
		File dir = new File(dataDir, host);
		dir.mkdirs();
		Writer out = new FileWriter(new File(dir, "hello"));
		out.write("world");
		out.close();
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testPUT() throws Exception {
		client.path("hello").put(String.class, "world");
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testRedirect() throws Exception {
		client.path("world").put("world");
		client.path("hello").header("Content-Location", client.path("world").getURI()).put();
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testDELETE() throws Exception {
		client.path("hello").put(String.class, "world");
		client.path("hello").delete();
		try {
			client.path("hello").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testGETIfModifiedSince() throws Exception {
		File dir = new File(dataDir, host);
		dir.mkdirs();
		Writer out = new FileWriter(new File(dir, "hello"));
		out.write("world");
		out.close();
		WebResource hello = client.path("hello");
		Date lastModified = hello.head().getLastModified();
		try {
			hello.header("If-Modified-Since", lastModified).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(304, e.getResponse().getStatus());
		}
		Thread.sleep(1000);
		out = new FileWriter(new File(dir, "hello"));
		out.write("bad world");
		out.close();
		assertEquals("bad world", hello.header("If-Modified-Since", lastModified).get(String.class));
	}

	public void testPUTIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put(String.class, "world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put(String.class, "new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).put(String.class, "bad world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testDELETEIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put(String.class, "world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put(String.class, "new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testPUTContentType() throws Exception {
		WebResource hello = client.path("hello");
		hello.header("Content-Type", "text/world").put(String.class, "world");
		assertEquals("text/world", hello.head().getMetadata().getFirst("Content-Type"));
	}
}
