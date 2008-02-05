package org.openrdf.alibaba.firefox;

import org.mortbay.jetty.Server;

public class JettyServer {
	public static void main(String[] args) throws Exception {
		JettyServerFactory factory = new JettyServerFactory();
		Server server = factory.createServer();
		server.start();
		server.join();
	}
}
