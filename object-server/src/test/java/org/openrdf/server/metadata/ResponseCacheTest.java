package org.openrdf.server.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.realm;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;
import org.openrdf.server.metadata.behaviours.PUTSupport;
import org.openrdf.server.metadata.concepts.Realm;

import com.sun.jersey.api.client.WebResource;

public class ResponseCacheTest extends MetadataServerTestCase {

	private WebResource display;
	private WebResource clock;
	private WebResource seq;

	@iri("urn:mimetype:application/clock")
	public static class Clock {
		@iri("urn:test:display")
		private Display display;

		@operation("display")
		public Display getDisplay() {
			return display;
		}

		@operation("display")
		public void setDisplay(Display display) {
			this.display = display;
		}

		@operation("date")
		public void setDate(String date) {
			display.setDate(date);
		}

		@operation("time")
		public void setTime(String time) {
			display.setTime(time);
		}
	}

	@iri("urn:mimetype:application/display")
	public interface Display {
		@operation("date")
		@cacheControl("max-age=3")
		@iri("urn:test:date")
		String getDate();

		void setDate(String date);

		@operation("time")
		@iri("urn:test:time")
		@cacheControl("no-cache")
		String getTime();

		void setTime(String time);
	}

	@iri("urn:mimetype:application/seq")
	public static class Seq {
		private static AtomicLong seq = new AtomicLong();
		@operation("next")
		@type("text/plain")
		public String next() {
			return Long.toHexString(seq.incrementAndGet());
		}
		@realm("/digest")
		@operation("auth")
		@type("text/plain")
		public String auth() {
			return Long.toHexString(seq.incrementAndGet());
		}
		@operation("number")
		@type("text/plain")
		@cacheControl("public")
		public String number() {
			return Long.toHexString(seq.incrementAndGet());
		}
		@operation("seq")
		@type("text/plain")
		@cacheControl("no-cache")
		public String seq() {
			return Long.toHexString(seq.incrementAndGet());
		}
	}

	public static class AnybodyRealm implements Realm {

		public String allowOrigin() {
			return null;
		}

		public boolean authorize(String addr, String method, String format,
				String algorithm, byte[] encoded) {
			return true;
		}

		public boolean authorize(String addr, String method, String url,
				String authorization, String format, String algorithm,
				byte[] encoded) {
			return true;
		}

		public InputStream unauthorized() throws IOException {
			return null;
		}
		
	}

	public void setUp() throws Exception {
		URIImpl type = new URIImpl("urn:test:AnybodyRealm");
		config.addConcept(Seq.class);
		config.addConcept(Clock.class);
		config.addConcept(Display.class);
		config.addBehaviour(AnybodyRealm.class, type);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
		ObjectConnection con = repository.getConnection();
		try {
			String uri = client.path("/digest").toString();
			con.addDesignations(con.getObject(uri), type);
		} finally {
			con.close();
		}
		seq = client.path("/seq");
		seq.type("application/seq").put("cocky");
		display = client.path("/display");
		display.type("application/display").put("display");
		clock = client.path("/clock");
		clock.type("application/clock").put("clock");
		clock.queryParam("display", "").header("Content-Location",
				display.getURI()).put();
	}

	public void testNoCache() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource time = display.queryParam("time", "");
		String now = time.get(String.class);
		clock.queryParam("time", "").put("later");
		assertFalse(now.equals(time.get(String.class)));
	}

	public void testNoAuthorization() throws Exception {
		WebResource next = seq.queryParam("next", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertFalse(first.equals(second));
	}

	public void testFirstAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertFalse(first.equals(second));
	}

	public void testSecondAuthorization() throws Exception {
		WebResource next = seq.queryParam("auth", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertFalse(first.equals(second));
	}

	public void testPublicNoAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertEquals(first, second);
	}

	public void testPublicFirstAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicSecondAuthorization() throws Exception {
		WebResource next = seq.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertEquals(first, second);
	}
}
