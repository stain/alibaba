package org.openrdf.server.metadata;

import java.io.IOException;
import java.nio.CharBuffer;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;
import org.openrdf.server.metadata.behaviours.PUTSupport;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class CharsetTest extends MetadataServerTestCase {

	public static class Resource {
		@operation("string")
		@type("text/plain")
		public String hello() {
			return "Hello World!";
		}

		@operation("stream")
		@type("text/plain;charset=UTF-8")
		public Readable stream() {
			return new Readable() {
				private boolean written;
				public int read(CharBuffer cb) throws IOException {
					if (written)
						return -1;
					written = true;
					cb.append("Hello World!");
					return "Hello World!".length();
				}
			};
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class, RDFS.RESOURCE);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	public void testCharsetString() throws Exception {
		client.path("/hello").put("resource");
		WebResource web = client.path("/hello").queryParam("string", "");
		ClientResponse put = web.type("text/plain;charset=UTF-8").put(
				ClientResponse.class, "put as utf8");
		String tag = put.getEntityTag().toString();
		ClientResponse get = web.header("Accept-Charset", "ISO-8859-1").get(
				ClientResponse.class);
		assertEquals("text/plain;charset=ISO-8859-1", get.getHeaders()
				.getFirst("Content-Type"));
		assertFalse(tag.equals(get.getEntityTag().toString()));
	}

	public void testCharsetStream() throws Exception {
		client.path("/hello").put("resource");
		WebResource web = client.path("/hello").queryParam("stream", "");
		ClientResponse get = web.header("Accept-Charset", "ISO-8859-1").get(
				ClientResponse.class);
		assertEquals("text/plain;charset=UTF-8", get.getHeaders().getFirst(
				"Content-Type"));
	}
}
