package org.openrdf.server.metadata;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class CharsetTest extends MetadataServerTestCase {

	public static class Resource {
		@operation("hello")
		@type("text/plain")
		public String hello() {
			return "Hello World!";
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testCharset() throws Exception {
		client.path("/hello").put("resource");
		WebResource web = client.path("/hello").queryParam("hello", "");
		ClientResponse put = web.type("text/plain;charset=UTF-8").put(ClientResponse.class, "put as utf8");
		String tag = put.getEntityTag().toString();
		ClientResponse get = web.header("Accept-Charset", "ISO-8859-1").get(ClientResponse.class);
		assertEquals("text/plain;charset=ISO-8859-1", get.getHeaders().getFirst("Content-Type"));
		assertFalse(tag.equals(get.getEntityTag().toString()));
	}
}
