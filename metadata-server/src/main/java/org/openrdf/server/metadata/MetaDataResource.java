package org.openrdf.server.metadata;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.server.metadata.annotations.OPTIONS;
import org.openrdf.server.metadata.annotations.TRACE;

import com.sun.jersey.api.core.ResourceContext;

@Path("{path:.*}")
public class MetaDataResource {
	private ObjectRepository repository;
	private File dataDir;

	public MetaDataResource(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
	}

	@GET
	public Response get(@Context Request request, @Context ResourceContext ctx,
			@Context Providers providers) throws Throwable {
		return subResource(request, ctx, providers).get();
	}

	@PUT
	public Response put(@Context Request request, @Context ResourceContext ctx,
			@Context Providers providers, @Context HttpHeaders headers,
			InputStream in) throws Throwable {
		return subResource(request, ctx, providers).put(headers, in);
	}

	@POST
	public Response post(@Context Request request, @Context ResourceContext ctx,
			@Context Providers providers, @Context HttpHeaders headers,
			InputStream in) throws Throwable {
		return subResource(request, ctx, providers).post(headers, in);
	}

	@DELETE
	public Response delete(@Context Request request,
			@Context ResourceContext ctx, @Context Providers providers)
			throws RepositoryException {
		return subResource(request, ctx, providers).delete();
	}

	@OPTIONS
	public Response options(@Context Request request,
			@Context ResourceContext ctx, @Context Providers providers,
			@Context UriInfo info) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		String path = info.getPath();
		if ("*".equals(path)) {
			sb.append("OPTIONS, TRACE, GET, HEAD, PUT, POST, DELETE");
		} else {
			sb.append("OPTIONS, TRACE");
			SubResource subResource = subResource(request, ctx, providers);
			for (String method : subResource.getAllowedMethods()) {
				sb.append(", ").append(method);
			}
			return Response.ok().header("Allow", sb.toString()).build();
		}
		return Response.ok().header("Allow", sb.toString()).build();
	}

	@TRACE
	public Response trace(@Context UriInfo info, @Context HttpHeaders headers)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("TRACE ").append(info.getRequestUri()).append("\r\n");
		for (Map.Entry<String, List<String>> e : headers.getRequestHeaders()
				.entrySet()) {
			for (String value : e.getValue()) {
				sb.append(e.getKey()).append(": ");
				sb.append(value).append("\r\n");
			}
		}
		sb.append("\r\n");
		return Response.ok().type("message/http").entity(sb.toString()).build();
	}

	private SubResource subResource(Request request, ResourceContext ctx,
			Providers providers) throws RepositoryException {
		ConnectionResource closer = ctx.getResource(ConnectionResource.class);
		ValueFactoryResource resolver = ctx.getResource(ValueFactoryResource.class);
		ObjectConnection con = repository.getConnection();
		con.setAutoCommit(false);
		closer.setConnection(con);
		resolver.setValueFactory(con.getValueFactory());
		resolver.setDataDir(dataDir);
		File file = resolver.getFile();
		URI uri = resolver.getURI();
		MultivaluedMap<String, String> params = resolver.getQueryParameters();
		if (!request.getMethod().equals("POST") && params.isEmpty()) {
			return new DataResource(request, ctx, providers, file, con, uri, params);
		} else {
			return new MetaResource(request, ctx, providers, file, con, uri, params);
		}
	}
}
