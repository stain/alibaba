package org.openrdf.server.metadata.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class PutResource extends MetadataResource {

	public PutResource(File file, RDFObject target) {
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
		WebResource target = addWebResourceDesignation();
		if (req.getHeader("Content-Type") == null && loc != null) {
			ObjectFactory of = con.getObjectFactory();
			URI uri = createURI(loc);
			WebResource redirect = of.createObject(uri, WebResource.class);
			target.setRedirect(redirect);
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
					target.setRedirect(null);
					target.setMediaType(contentType);
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
		List<Method> methods = findSetterMethods(name);
		if (methods.isEmpty())
			return methodNotAllowed(req);
		Method method = findBestMethod(req, methods);
		if (method == null)
			return new Response().badRequest();
		try {
			// invoke method
			invoke(method, req);
			// save any changes made
			getObjectConnection().setAutoCommit(true);
			return new Response().noContent();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
