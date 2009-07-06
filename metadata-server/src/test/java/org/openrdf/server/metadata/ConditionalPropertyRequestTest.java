package org.openrdf.server.metadata;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ConditionalPropertyRequestTest extends MetadataServerTestCase {

	@rdf(RDFS.NAMESPACE + "Resource")
	public interface Resource {
		@operation("property")
		@type("text/plain")
		@rdf("urn:test:property")
		String getProperty();
		@operation("property")
		void setProperty(String property);
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class);
		super.setUp();
	}

	public void testRefresh() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		assertEquals("server", web.header("If-None-Match", tag).get(String.class));
	}

	public void testRefreshFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		try {
			web.header("If-None-Match", tag).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(304, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testValidate() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		assertEquals("world", web.header("If-Match", tag).get(String.class));
	}

	public void testValidateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		try {
			web.header("If-Match", tag).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("server", web.get(String.class));
	}

	public void testCreate() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.header("If-None-Match", "*").put("world");
		assertEquals("world", web.get(String.class));
	}

	public void testCreateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		try {
			web.header("If-None-Match", "*").put("server");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testUpdate() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		web.header("If-Match", "*").put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		try {
			web.header("If-Match", "*").put("world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testUpdateMatch() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateMatchFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").put("server");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testDelete() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		web.header("If-Match", "*").delete();
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		try {
			web.header("If-Match", "*").delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteMatch() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).delete();
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteMatchFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}
}
