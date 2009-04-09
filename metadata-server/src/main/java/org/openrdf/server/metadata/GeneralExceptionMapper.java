package org.openrdf.server.metadata;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {
	public Response toResponse(Exception ex) {
		if (ex instanceof WebApplicationException) {
			return ((WebApplicationException) ex).getResponse();
		} else {
			ex.printStackTrace();
			return Response.serverError().entity(ex.getMessage()).type(
					"text/plain").build();
		}
	}
}
