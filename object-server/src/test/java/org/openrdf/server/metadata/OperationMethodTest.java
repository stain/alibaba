package org.openrdf.server.metadata;

import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.base.MetadataServerTestCase;

import com.sun.jersey.api.client.WebResource;

public class OperationMethodTest extends MetadataServerTestCase {

	public static class Resource1 {
		public static String operation;

		@operation("op1")
		public String getOperation1() {
			return operation;
		}

		@operation("op1")
		@method("PUT")
		public void setOperation1(String value) {
			operation = String.valueOf(value);
		}

		@operation("op1")
		@method("DELETE")
		public void delOperation1() {
			operation = null;
		}

		@operation("op1")
		public String setAndGetOperation1(String value) {
			String pre = operation;
			operation = value;
			return pre;
		}
	}

	public static class Resource2 {
		public static String operation;

		@operation("op2")
		public String getOperation2() {
			return operation;
		}

		@operation("op2")
		@method("PUT")
		public void setOperation2(String value) {
			operation = String.valueOf(value);
		}

		@operation("op2")
		@method("DELETE")
		public void delOperation2() {
			operation = null;
		}

		@operation("op2")
		public String setAndGetOperation2(String value) {
			String pre = operation;
			operation = value;
			return pre;
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Resource1.class, RDFS.RESOURCE);
		config.addBehaviour(Resource2.class, RDFS.RESOURCE);
		super.setUp();
		Resource1.operation = "op1";
		Resource2.operation = "op2";
	}

	public void testGetOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		assertEquals("op1", op1.get(String.class));
		assertEquals("op2", op2.get(String.class));
	}

	public void testPutOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		op1.put("put1");
		op2.put("put2");
		assertEquals("put1", Resource1.operation);
		assertEquals("put2", Resource2.operation);
	}

	public void testDeleteOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		op1.delete();
		op2.delete();
		assertNull(Resource1.operation);
		assertNull(Resource2.operation);
	}

	public void testPostOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		assertEquals("op1", op1.post(String.class, "post1"));
		assertEquals("op2", op2.post(String.class, "post2"));
		assertEquals("post1", op1.get(String.class));
		assertEquals("post2", op2.get(String.class));
	}
}
