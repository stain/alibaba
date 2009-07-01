package org.openrdf.server.metadata;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RequestCacheTest extends MetadataServerTestCase {

	private WebResource display;
	private WebResource clock;

	@rdf("urn:mimetype:application/clock")
	public static class Clock {
		@rdf("urn:test:display")
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

	@rdf("urn:mimetype:application/display")
	public interface Display {
		@operation("date")
		@cacheControl("max-age=3")
		@rdf("urn:test:date")
		String getDate();

		void setDate(String date);

		@operation("time")
		@rdf("urn:test:time")
		String getTime();

		void setTime(String time);

		@rel("alternate")
		@operation("construct")
		@sparql("DESCRIBE $this")
		GraphQueryResult construct();

		@rel("alternate")
		@operation("select")
		@sparql("SELECT ?date ?time WHERE { $this <urn:test:date> ?date ; <urn:test:time> ?time }")
		TupleQueryResult select();
	}

	public void setUp() throws Exception {
		config.addConcept(Clock.class);
		config.addConcept(Display.class);
		super.setUp();
		display = client.path("/display");
		display.type("application/display").put("display");
		clock = client.path("/clock");
		clock.type("application/clock").put("clock");
		clock.queryParam("display", "").header("Content-Location",
				display.getURI()).put();
	}

	public void testStale() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource time = display.queryParam("time", "");
		String now = time.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("time", "").put("later");
		assertFalse(now.equals(time.get(String.class)));
	}

	public void testResponseMaxAge() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertEquals(now, date.get(String.class));
	}

	public void testRequestMaxAge() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource time = display.queryParam("date", "");
		String now = time.get(String.class);
		Thread.sleep(2000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(time.header("cache-control", "max-age=1").get(
				String.class)));
	}

	public void testRequestNoCache() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(date.header("cache-control", "no-cache").get(
				String.class)));
	}

	public void testMaxStale() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource date = display.queryParam("time", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("time", "").put("later");
		assertEquals(now, date.header("cache-control", "max-stale").get(
				String.class));
	}

	public void testMinFresh() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertFalse(now.equals(date.header("cache-control", "min-fresh=3").get(
				String.class)));
	}

	public void testOnlyCached() throws Exception {
		clock.queryParam("date", "").put("earlier");
		WebResource date = display.queryParam("date", "");
		String now = date.get(String.class);
		Thread.sleep(1000);
		clock.queryParam("date", "").put("later");
		assertEquals(now, date.header("cache-control", "only-if-cached").get(
				String.class));
	}

	public void testOnlyNotCached() throws Exception {
		clock.queryParam("time", "").put("earlier");
		WebResource date = display.queryParam("time", "");
		Thread.sleep(1000);
		clock.queryParam("time", "").put("later");
		try {
			date.header("cache-control", "only-if-cached").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(504, e.getResponse().getStatus());
		}
	}

	public void testSeeAlso() throws Exception {
		ClientResponse tuple = display.accept("application/sparql-results+xml").get(ClientResponse.class);
		assertEquals("application/sparql-results+xml;charset=UTF-8", tuple.getType().toString());
		String tupleTag = tuple.getEntityTag().toString();
		ClientResponse graph = display.accept("application/rdf+xml").get(ClientResponse.class);
		assertEquals("application/rdf+xml;charset=UTF-8", graph.getType().toString());
		String graphTag = graph.getEntityTag().toString();
		assertFalse(tupleTag.equals(graphTag));
	}

}
