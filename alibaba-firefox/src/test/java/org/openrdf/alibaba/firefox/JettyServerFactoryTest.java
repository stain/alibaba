package org.openrdf.alibaba.firefox;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;

public class JettyServerFactoryTest extends TestCase {
	public void testCreateServer() throws Exception {
		JettyServerFactory factory = new JettyServerFactory();
		Server server = factory.createServer();
		server.start();
		//server.join();
		server.stop();
	}
}
