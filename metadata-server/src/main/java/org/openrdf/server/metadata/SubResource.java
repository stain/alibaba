package org.openrdf.server.metadata;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Providers;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;

import com.sun.jersey.api.core.ResourceContext;

public abstract class SubResource {
	protected ObjectConnection con;
	protected URI uri;
	protected File file;
	protected ValueFactoryResource vf;
	protected ObjectFactory of;
	protected Request request;
	protected ResourceContext ctx;
	protected Providers providers;
	protected MultivaluedMap<String, String> params;

	public SubResource(Request request, ResourceContext ctx,
			Providers providers, File file, ObjectConnection con, URI uri,
			MultivaluedMap<String, String> params) {
		this.request = request;
		this.ctx = ctx;
		this.providers = providers;
		this.file = file;
		this.con = con;
		this.vf = ctx.getResource(ValueFactoryResource.class);
		this.of = con.getObjectFactory();
		this.uri = uri;
		this.params = params;
	}

	public abstract ResponseBuilder get() throws Throwable;

	public abstract ResponseBuilder put(HttpHeaders headers, InputStream in)
			throws Throwable;

	public ResponseBuilder post(HttpHeaders headers, InputStream in) throws Throwable {
		return methodNotAllowed();
	}

	public abstract ResponseBuilder delete() throws Throwable;

	public abstract Set<String> getAllowedMethods() throws RepositoryException;

	protected ResponseBuilder methodNotAllowed() throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods()) {
			sb.append(", ").append(method);
		}
		return Response.status(405).header("Allow", sb.toString());
	}
}
