package org.openrdf.alibaba.firefox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;

/**
 * Creates a Jetty {@link Server} from the resource file
 * <code>"META-INF/jetty.xml"</code>.
 * 
 * @author James Leigh
 * 
 */
public class JettyServerFactory {
	public Server createServer() throws Exception {
		try {
			ClassLoader cl = JettyServerFactory.class.getClassLoader();
			URL jettyXml = cl.getResource("META-INF/jetty.xml");
			XmlConfiguration configuration = new XmlConfiguration(jettyXml);
			Server server = new Server();
			configuration.configure(server);
			return server;
		} catch (Exception e) {
			StringWriter out = new StringWriter();
			e.printStackTrace(new PrintWriter(out));
			throw new AssertionError(out.toString());
		}
	}
}
