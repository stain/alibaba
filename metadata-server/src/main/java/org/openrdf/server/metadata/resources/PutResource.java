package org.openrdf.server.metadata.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class PutResource extends MetadataResource {

	public PutResource(File file, WebResource target) {
		super(file, target);
	}

	public Response put(Request req) throws Throwable {
		String name = req.getOperation();
		if (name == null) {
			return putData(req);
		} else {
			return putMetadata(req, name);
		}
	}

	private Response putData(Request req) throws RepositoryException,
			IOException {
		File file = getFile();
		ObjectConnection con = getObjectConnection();
		String loc = req.getHeader("Content-Location");
		if (req.getHeader("Content-Type") == null && loc != null) {
			ObjectFactory of = con.getObjectFactory();
			URI uri = createURI(loc);
			WebResource redirect = of.createObject(uri, WebResource.class);
			getWebResource().setRedirect(redirect);
			con.setAutoCommit(true);
			return new Response().noContent();
		}
		try {
			// TODO use file locks to prevent conflicts
			File dir = file.getParentFile();
			dir.mkdirs();
			File tmp = new File(dir, file.getName() + ".part");
			InputStream in = req.getInputStream();
			OutputStream out = new FileOutputStream(tmp);
			try {
				byte[] buf = new byte[512];
				int read;
				while ((read = in.read(buf)) >= 0) {
					out.write(buf, 0, read);
				}
				if (!tmp.renameTo(file)) {
					tmp.delete();
					return methodNotAllowed(req);
				}
				String contentType = req.getHeader("Content-Type");
				if (contentType != null) {
					getWebResource().setMediaType(contentType);
					con.setAutoCommit(true);
				}
				return new Response().noContent();
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			return methodNotAllowed(req);
		}
	}

	private Response putMetadata(Request req, String name)
			throws RepositoryException, IOException, IllegalAccessException,
			Throwable {
		// lookup method
		Method method = findSetterMethod(name);
		if (method == null)
			return methodNotAllowed(req);
		try {
			// invoke method
			invoke(method, req);
			// save any changes made
			getWebResource().getObjectConnection().setAutoCommit(true);
			return new Response().noContent();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
