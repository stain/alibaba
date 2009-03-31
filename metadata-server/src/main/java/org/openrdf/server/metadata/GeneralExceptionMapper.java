package org.openrdf.server.metadata;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
	public Response toResponse(Exception ex) {
		ex.printStackTrace();
		return Response.serverError().entity(ex.getMessage())
				.type("text/plain").build();
	}
}
