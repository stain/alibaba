package org.openrdf.server.metadata;

import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ConditionalDataRequestTest extends MetadataServerTestCase {

	public void testCreate() throws Exception {
		WebResource web = client.path("/hello");
		web.header("If-None-Match", "*").put("world");
		assertEquals("world", web.get(String.class));
	}

	public void testCreateFail() throws Exception {
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
		web.put("world");
		web.header("If-Match", "*").put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateFail() throws Exception {
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateMatchFail() throws Exception {
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
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
		WebResource web = client.path("/hello");
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