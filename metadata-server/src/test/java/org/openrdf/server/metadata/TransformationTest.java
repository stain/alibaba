package org.openrdf.server.metadata;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public class TransformationTest extends MetadataServerTestCase {

	public static abstract class Service {
		@operation("hello")
		@type("text/plain")
		@cacheControl("no-transform")
		public String world() {
			return "hello world!";
		}

		@operation("hlo")
		@type("text/plain")
		public String hlo() {
			return "hello world!";
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Service.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testTransformation() {
		WebResource client = Client.create().resource("http://" + host);
		WebResource service = client.path("service").queryParam("hlo", "");
		service.addFilter(new GZIPContentEncodingFilter(true));
		assertEquals("hello world!", service.get(String.class));
	}

	public void testNoTransformationResponse() {
		WebResource client = Client.create().resource("http://" + host);
		WebResource service = client.path("service").queryParam("hello", "");
		assertEquals("hello world!", service.header("Accept-Encoding", "gzip")
				.get(String.class));
	}

	public void testNoTransformationRequest() {
		WebResource client = Client.create().resource("http://" + host);
		WebResource service = client.path("service").queryParam("hlo", "");
		assertEquals("hello world!", service.header("Accept-Encoding", "identity,*;q=0")
				.get(String.class));
	}
}
