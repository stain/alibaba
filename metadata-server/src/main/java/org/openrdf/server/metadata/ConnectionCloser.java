package org.openrdf.server.metadata;

import javax.annotation.PreDestroy;
import javax.ws.rs.GET;

import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.resource.PerRequest;

@PerRequest
public class ConnectionCloser {
	private Logger logger = LoggerFactory.getLogger(ConnectionCloser.class);
	private ObjectConnection connection;

	public void closeAfterResponse(ObjectConnection con) {
		this.connection = con;
	}

	@GET
	public String toString() {
		return connection.toString();
	}

	@PreDestroy
	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}

}
