package org.openrdf.server.metadata;

import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.WebResource;

public class ResponseCacheTest extends MetadataServerTestCase {

	@rdf("urn:mimetype:application/clock")
	public static class Clock {
		private static AtomicLong seq = new AtomicLong();
		@operation("next")
		@type("text/plain")
		public String next() {
			return Long.toHexString(seq.incrementAndGet());
		}
		@operation("number")
		@type("text/plain")
		@cacheControl("public")
		public String number() {
			return Long.toHexString(seq.incrementAndGet());
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Clock.class);
		super.setUp();
	}

	public void testNoAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("next", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("next", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertFalse(first.equals(second));
	}

	public void testFirstAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("next", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertFalse(first.equals(second));
	}

	public void testSecondAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("next", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertFalse(first.equals(second));
	}

	public void testPublicNoAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertEquals(first, second);
	}

	public void testPublicFirstAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("number", "");
		String first = next.header("Authorization", "first").get(String.class);
		String second = next.get(String.class);
		assertEquals(first, second);
	}

	public void testPublicSecondAuthorization() throws Exception {
		WebResource clock = client.path("/clock");
		clock.type("application/clock").put("cocky");
		WebResource next = clock.queryParam("number", "");
		String first = next.get(String.class);
		String second = next.header("Authorization", "second").get(String.class);
		assertEquals(first, second);
	}
}
