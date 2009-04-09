package org.openrdf.server.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.store.StoreException;

import com.sun.jersey.api.NotFoundException;

import eu.medsea.util.MimeUtil;

public class DataResource {
	private static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/04/metadata#";
	private static URI CONTENT_TYPE = new URIImpl(NAMESPACE + "conentType");

	private ObjectConnection con;
	private URI uri;
	private File file;

	public DataResource(ObjectConnection con, URI uri, File file) {
		this.con = con;
		this.uri = uri;
		this.file = file;
	}

	@GET
	public Response get(@Context Request request) throws Throwable {
		ResponseBuilder rb;
		if (file.canRead()) {
			Date last = new Date(file.lastModified());
			rb = request.evaluatePreconditions(last);
			if (rb == null) {
				rb = Response.ok();
				rb.lastModified(last);
				rb.type(getContentType());
				try {
					rb.entity(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					throw new NotFoundException("Not Found <"
							+ uri.stringValue() + ">");
				}
			}
		} else if (file.exists()) {
			return methodNotAllowed(file);
		} else if (con.hasMatch(uri, null, null)
				|| con.hasMatch(null, null, null, uri)) {
			java.net.URI loc = new java.net.URI(uri.stringValue() + "?describe");
			rb = Response.status(303).location(loc);
		} else {
			throw new NotFoundException("Not Found <" + uri.stringValue() + ">");
		}
		return rb.build();
	}

	@PUT
	public Response put(@Context Request request, @Context HttpHeaders headers,
			InputStream in) throws IOException, StoreException {
		Date last = new Date(file.lastModified());
		ResponseBuilder rb = request.evaluatePreconditions(last);
		if (rb == null) {
			try {
				file.getParentFile().mkdirs();
				OutputStream out = new FileOutputStream(file);
				try {
					byte[] buf = new byte[512];
					int read;
					while ((read = in.read(buf)) >= 0) {
						out.write(buf, 0, read);
					}
					rb = Response.ok();
				} finally {
					out.close();
				}
			} catch (FileNotFoundException e) {
				return methodNotAllowed(file);
			}
			MultivaluedMap<String, String> map = headers.getRequestHeaders();
			String contentType = map.getFirst("Content-Type");
			if (contentType != null) {
				ValueFactory vf = con.getValueFactory();
				con.begin();
				con.removeMatch(uri, CONTENT_TYPE, null);
				con.add(uri, CONTENT_TYPE, vf.createLiteral(contentType));
				con.commit();
			}
		}
		return rb.build();
	}

	public Response delete(@Context Request request) throws StoreException {
		if (!file.exists())
			throw new NotFoundException("Not Found");
		Date last = new Date(file.lastModified());
		ResponseBuilder rb = request.evaluatePreconditions(last);
		if (rb != null)
			return rb.build();
		if (!file.delete())
			return methodNotAllowed(file);
		return Response.noContent().build();
	}

	private Response methodNotAllowed(File file) throws StoreException {
		StringBuilder sb = new StringBuilder();
		if (file.canRead()) {
			sb.append("GET, HEAD");
		}
		if (file.canWrite()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("PUT");
		}
		if (file.getParentFile().canWrite()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("DELETE");
		}
		for (Method m : con.getObject(uri).getClass().getMethods()) {
			if (m.isAnnotationPresent(operation.class)) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("POST");
				break;
			}
		}
		return Response.status(405).header("Allow", sb.toString()).build();
	}

	private String getContentType() throws StoreException {
		Model types = con.match(uri, CONTENT_TYPE, null, true).asModel();
		for (Value type : types.objects()) {
			return type.stringValue();
		}
		String mimeType = MimeUtil.getMagicMimeType(file);
		if (mimeType == null)
			return MediaType.APPLICATION_OCTET_STREAM;
		con.begin();
		try {
			types = con.match(uri, CONTENT_TYPE, null, true).asModel();
			for (Value type : types.objects()) {
				return type.stringValue();
			}
			Literal lit = con.getValueFactory().createLiteral(mimeType);
			con.add(uri, CONTENT_TYPE, lit);
			con.commit();
		} finally {
			if (!con.isAutoCommit()) {
				con.rollback();
			}
		}
		return mimeType;
	}
}
