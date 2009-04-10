package org.openrdf.server.metadata;

import java.io.File;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.ResourceContext;

@Path("/")
public class MetaDataResource {
	private ObjectRepository repository;
	private File dataDir;

	public MetaDataResource(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
	}

	@Path("{path:.*}")
	public Object request(@Context UriInfo info, @Context Request request,
			@Context ResourceContext ctx) throws Throwable {
		MultivaluedMap<String, String> params = info.getQueryParameters();
		ConnectionCloser closer = ctx.getResource(ConnectionCloser.class);
		ObjectConnection con = repository.getConnection();
		closer.closeAfterResponse(con);
		java.net.URI net = info.getAbsolutePath();
		File file = getFile(net);
		URI uri = con.getValueFactory().createURI(net.toASCIIString());
		if (request.getMethod().equals("POST")) {
			return con.getObject(uri);
		} else if (request.getMethod().equals("DELETE")) {
			return new DeleteResource(con, uri, file);
		} else if (params.isEmpty()) {
			return new DataResource(con, uri, file);
		} else {
			return new MetaResource(con, uri);
		}
	}

	public static class DeleteResource {
		private ObjectConnection con;
		private URI uri;
		private File file;

		public DeleteResource(ObjectConnection con, URI uri, File file) {
			this.con = con;
			this.uri = uri;
			this.file = file;
		}

		@DELETE
		public Response delete(@Context Request request) throws StoreException {
			try {
				Response rb = new DataResource(con, uri, file).delete(request);
				if (rb.getStatus() >= 300)
					return rb;
			} catch (NotFoundException e) {
				// skip
			}
			new MetaResource(con, uri).delete();
			return Response.noContent().build();
		}
	}

	public File getFile(java.net.URI uri) {
		String host = uri.getAuthority();
		File base = new File(dataDir, host);
		File file = new File(base, uri.getPath());
		if (file.isFile())
			return file;
		return new File(file, Integer.toHexString(uri.toString().hashCode()));
	}
}
