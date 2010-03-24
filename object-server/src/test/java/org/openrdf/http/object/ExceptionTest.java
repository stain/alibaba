package org.openrdf.http.object;

import java.io.OutputStream;

import javax.xml.stream.XMLEventReader;

import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.matches;

import com.sun.jersey.api.client.UniformInterfaceException;

public class ExceptionTest extends MetadataServerTestCase {

	@matches("/")
	public static class Brake {
		@iri("urn:test:invalid")
		protected Boolean invalid;

		public Boolean getInvalid() {
			return invalid;
		}

		public void setInvalid(Boolean invalid) {
			this.invalid = invalid;
		}

		@operation("bad")
		public String badRequest(String body) throws Exception {
			setInvalid(true);
			throw new BadRequest("this in a bad request");
		}

		@operation("exception")
		public String throwException(String body) throws Exception {
			setInvalid(true);
			throw new Exception("this in an exception");
		}

		@operation("stream")
		public OutputStream getStream() {
			return System.out;
		}

		@operation("xml")
		public String postXML(@type("application/xml") XMLEventReader xml) {
			return "xml";
		}

		@operation("rdf")
		public void setObject(Object o) {

		}
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Brake.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testBrake() throws Exception {
		try {
			client.path("/").queryParam("exception", "").post(String.class,
					"input");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(500, e.getResponse().getStatus());
		}
	}

	public void testRollbackException() throws Exception {
		try {
			client.path("/").queryParam("exception", "").post(String.class,
					"input");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(500, e.getResponse().getStatus());
		}
		ObjectConnection con = repository.getConnection();
		try {
			Brake brake = (Brake) con.getObject(client.path("/").toString());
			assertNull(brake.getInvalid());
		} finally {
			con.close();
		}
	}

	public void testBadRollback() throws Exception {
		try {
			client.path("/").queryParam("bad", "").post(String.class, "input");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
		ObjectConnection con = repository.getConnection();
		try {
			Brake brake = (Brake) con.getObject(client.path("/").toString());
			assertNull(brake.getInvalid());
		} finally {
			con.close();
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

	public void testUnsupportedMediaType() throws Exception {
		try {
			client.path("/").queryParam("xml", "").type("message/http").post(
					String.class, "GET / HTTP/1.0\r\n\r\n");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(415, e.getResponse().getStatus());
		}
	}

	public void testBadRequest() throws Exception {
		try {
			client.path("/").queryParam("rdf", "").type("application/rdf+xml")
					.put("garbage");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(400, e.getResponse().getStatus());
		}
	}

}
