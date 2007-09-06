package org.openrdf.alibaba.servlet.impl;


import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.openrdf.alibaba.servlet.helpers.MockRequest;
import org.openrdf.alibaba.servlet.helpers.MockResponse;
import org.openrdf.alibaba.servlet.helpers.MockStateManager;

public class AlibabaServletTest extends TestCase {
	private AlibabaServlet servlet;
	private MockStateManager manager;

	public void testResource() throws Exception {
		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();
		req.setRequestURL("http://localhost/ali/target");
		req.setPathInfo("/ali/target");
		req.setMethod("PUT");
		servlet.service(req, resp);
		assertEquals("", new String(resp.getContent()));
		assertEquals(204, resp.getStatusCode());
		assertEquals("ali", manager._resource.getPrefix());
		assertEquals("target", manager._resource.getLocalPart());
		assertNull(manager._intention);
	}

	public void testIntention() throws Exception {
		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();
		req.setRequestURL("http://localhost/crud/update/ali/target");
		req.setPathInfo("/crud/update/ali/target");
		req.setMethod("PUT");
		servlet.service(req, resp);
		assertEquals("", new String(resp.getContent()));
		assertEquals(204, resp.getStatusCode());
		assertEquals("ali", manager._resource.getPrefix());
		assertEquals("target", manager._resource.getLocalPart());
		assertEquals("crud", manager._intention.getPrefix());
		assertEquals("update", manager._intention.getLocalPart());
	}

	public void testLocation() throws Exception {
		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();
		req.setRequestURL("http://localhost/ali/type");
		req.setPathInfo("/ali/type");
		req.setMethod("POST");
		manager._resource = new QName("urn:ali/", "target", "ali");
		servlet.service(req, resp);
		assertEquals("", new String(resp.getContent()));
		assertEquals(201, resp.getStatusCode());
		assertEquals("http://localhost/ali/target", resp.getHeader("Location"));
	}

	public void testContentLocation() throws Exception {
		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();
		req.setRequestURL("http://localhost/ali/type");
		req.setPathInfo("/ali/type");
		req.setMethod("POST");
		req.setHeader("Content-Location", "http://localhost/ali/target");
		manager._resource = new QName("urn:ali/", "target", "ali");
		servlet.service(req, resp);
		assertEquals("", new String(resp.getContent()));
		assertEquals(201, resp.getStatusCode());
		assertEquals("http://localhost/ali/target", resp.getHeader("Location"));
		assertEquals("ali", manager._resource.getPrefix());
		assertEquals("target", manager._resource.getLocalPart());
		assertNull(manager._intention);
	}

	@Override
	protected void setUp() throws Exception {
		manager = new MockStateManager();
		servlet = new AlibabaServlet();
		servlet.setStateManager(manager);
	}
}
