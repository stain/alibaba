package org.openrdf.http.object;

import java.io.OutputStream;

import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.UniformInterfaceException;

public class ExceptionTest extends MetadataServerTestCase {

	public static class Brake {
		@operation("exception")
		public String throwException() throws Exception {
			throw new Exception("this in an exception");
		}

		@operation("stream")
		public OutputStream getStream() {
			return System.out;
		}

		@operation("rdf")
		public void setObject(Object o) {
			
		}
	}

	@Override
	public void setUp() throws Exception {
		config.addBehaviour(Brake.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testBrake() throws Exception {
		try {
			client.path("/").queryParam("exception", "").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(500, e.getResponse().getStatus());
		}
	}

	public void testNotAcceptable() throws Exception {
		try {
			client.path("/").queryParam("stream", "").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(406, e.getResponse().getStatus());
		}
	}

	public void testBadRequest() throws Exception {
		try {
			client.path("/").queryParam("rdf", "").type("application/rdf+xml").put("garbage");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}

}
