package org.openrdf.server.metadata;

import java.io.File;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

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
	public Object request(@Context Request request, @Context ResourceContext ctx)
			throws StoreException {
		ConnectionCloser closer = ctx.getResource(ConnectionCloser.class);
		URIResolver resolver = ctx.getResource(URIResolver.class);
		ObjectConnection con = repository.getConnection();
		closer.closeAfterResponse(con);
		resolver.setValueFactory(con.getValueFactory());
		resolver.setDataDir(dataDir);
		File file = resolver.getFile();
		URI uri = resolver.getURI();
		MultivaluedMap<String, String> params = resolver.getQueryParameters();
		if (request.getMethod().equals("POST")) {
			return con.getObject(uri);
		} else if (request.getMethod().equals("DELETE")) {
			return new DeleteResource(con, uri, file, params);
		} else if (params.isEmpty()) {
			return new DataResource(con, uri, file);
		} else {
			return new MetaResource(con, uri, params);
		}
	}

	public static class DeleteResource {
		private ObjectConnection con;
		private URI uri;
		private File file;
		private MultivaluedMap<String, String> params;

		public DeleteResource(ObjectConnection con, URI uri, File file,
				MultivaluedMap<String, String> params) {
			this.con = con;
			this.uri = uri;
			this.file = file;
			this.params = params;
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
			new MetaResource(con, uri, params).delete();
			return Response.noContent().build();
		}
	}
}
