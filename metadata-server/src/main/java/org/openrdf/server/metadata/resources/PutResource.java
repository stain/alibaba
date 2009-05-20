package org.openrdf.server.metadata.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.model.URI;
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
		Response resp = invokeMethod(req, false);
		if (resp != null) {
			return resp;
		}
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
					target = setMediaType(contentType);
					URI uri = getURI();
					con.clear(uri);
					con.setAddContexts(uri);
					target.extractMetadata(file);
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

}
