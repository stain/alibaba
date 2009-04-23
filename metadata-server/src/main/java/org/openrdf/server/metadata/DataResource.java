package org.openrdf.server.metadata;

import info.aduna.iteration.Iterations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Providers;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.ResourceContext;

import eu.medsea.util.MimeUtil;

public class DataResource extends SubResource {
	private static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/meta#";
	private static URI MEDIA_TYPE = new URIImpl(NAMESPACE + "mediaType");
	private static URI REDIRECT = new URIImpl(NAMESPACE + "redirect");

	public DataResource(Request request, ResourceContext ctx,
			Providers providers, File file, ObjectConnection con, URI uri,
			MultivaluedMap<String, String> params) {
		super(request, ctx, providers, file, con, uri, params);
	}

	public ResponseBuilder get() throws RepositoryException {
		ResponseBuilder rb;
		List<Statement> redirect;
		if (file.canRead()) {
			Date last = new Date(file.lastModified());
			rb = request.evaluatePreconditions(last);
			if (rb == null) {
				rb = Response.ok();
				rb.lastModified(last);
				rb.type(getContentType());
				rb.entity(file);
			}
		} else if (file.exists()) {
			return methodNotAllowed();
		} else if (!(redirect = con.getStatements(uri, REDIRECT, null).asList())
				.isEmpty()) {
			String obj = redirect.get(0).getObject().stringValue();
			rb = Response.status(307).location(java.net.URI.create(obj));
		} else if (con.hasStatement((Resource) null, null, null, uri)) {
			java.net.URI loc = java.net.URI.create(uri.stringValue()
					+ "?named-graph");
			rb = Response.status(302).location(loc);
		} else if (con.hasStatement(uri, null, null)) {
			java.net.URI loc = java.net.URI.create(uri.stringValue()
					+ "?describe");
			rb = Response.status(303).location(loc);
		} else {
			throw new NotFoundException("Not Found <" + uri.stringValue() + ">");
		}
		return rb;
	}

	public ResponseBuilder put(HttpHeaders headers, InputStream in)
			throws IOException, RepositoryException {
		Date last = new Date(file.lastModified());
		ResponseBuilder rb = request.evaluatePreconditions(last);
		List<String> contentLocation = headers
				.getRequestHeader("Content-Location");
		if (rb == null && headers.getMediaType() == null
				&& contentLocation != null) {
			con.add(uri, REDIRECT, vf.createURI(contentLocation.get(0)));
			con.setAutoCommit(true);
			rb = Response.noContent();
		} else if (rb == null) {
			try {
				file.getParentFile().mkdirs();
				OutputStream out = new FileOutputStream(file);
				try {
					byte[] buf = new byte[512];
					int read;
					while ((read = in.read(buf)) >= 0) {
						out.write(buf, 0, read);
					}
					rb = Response.noContent();
				} finally {
					out.close();
				}
			} catch (FileNotFoundException e) {
				return methodNotAllowed();
			}
			MultivaluedMap<String, String> map = headers.getRequestHeaders();
			String contentType = map.getFirst("Content-Type");
			if (contentType != null) {
				con.remove(uri, MEDIA_TYPE, null);
				con.add(uri, MEDIA_TYPE, vf.createLiteral(contentType));
				con.setAutoCommit(true);
			}
		}
		return rb;
	}

	public ResponseBuilder delete() throws RepositoryException {
		if (!file.exists())
			throw new NotFoundException("Not Found");
		Date last = new Date(file.lastModified());
		ResponseBuilder rb = request.evaluatePreconditions(last);
		if (rb != null)
			return rb;
		if (!file.delete())
			return methodNotAllowed();
		con.remove(uri, MEDIA_TYPE, null);
		con.remove(uri, REDIRECT, null);
		con.setAutoCommit(true);
		return Response.noContent();
	}

	public Set<String> getAllowedMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		if (file.canRead()) {
			set.add("GET");
			set.add("HEAD");
		}
		File parent = file.getParentFile();
		if (file.canWrite() || !file.exists() && (!parent.exists() || parent.canWrite())) {
			set.add("PUT");
		}
		if (file.exists() && parent.canWrite()) {
			set.add("DELETE");
		}
		MetaResource meta = new MetaResource(request, ctx, providers, file,
				con, uri, params);
		Set<String> allowed = meta.getAllowedMethods();
		if (allowed.contains("POST")) {
			set.add("POST");
		}
		return set;
	}

	private String getContentType() throws RepositoryException {
		List<Statement> types = Iterations.asList(con.getStatements(uri,
				MEDIA_TYPE, null, true));
		for (Statement st : types) {
			return st.getObject().stringValue();
		}
		String mimeType = MimeUtil.getMagicMimeType(file);
		if (mimeType == null)
			return MediaType.APPLICATION_OCTET_STREAM;
		con.setAutoCommit(false);
		try {
			types = Iterations.asList(con.getStatements(uri, MEDIA_TYPE, null,
					true));
			for (Statement st : types) {
				return st.getObject().stringValue();
			}
			Literal lit = con.getValueFactory().createLiteral(mimeType);
			con.add(uri, MEDIA_TYPE, lit);
			con.setAutoCommit(true);
		} finally {
			if (!con.isAutoCommit()) {
				con.rollback();
				con.setAutoCommit(true);
			}
		}
		return mimeType;
	}
}
