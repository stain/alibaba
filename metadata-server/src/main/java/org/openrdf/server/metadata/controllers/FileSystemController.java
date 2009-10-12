package org.openrdf.server.metadata.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import javax.activation.MimeTypeParseException;

import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.WebObject;
import org.openrdf.server.metadata.concepts.InternalWebObject;
import org.openrdf.server.metadata.concepts.WebRedirect;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

public class FileSystemController {

	public boolean existsAndAcceptable(Request req)
			throws MimeTypeParseException, RepositoryException,
			QueryEvaluationException {
		if (!req.getFile().canRead())
			return false;
		InternalWebObject target = (InternalWebObject) req.getRequestedResource();
		String mediaType = target.getMediaType();
		if (mediaType == null) {
			mediaType = target.getAndSetMediaType();
			req.flush();
		}
		return req.isAcceptable(mediaType);
	}

	public Response get(Request req) throws MethodNotAllowed,
			RepositoryException, QueryEvaluationException,
			MimeTypeParseException {
		File file = req.getFile();
		WebObject target = req.getRequestedResource();
		Response rb = new Response();
		if (target instanceof WebRedirect) {
			String obj = ((WebRedirect) target).getRedirect().getResource()
					.stringValue();
			return rb.status(307).location(obj);
		}
		if (file.canRead()) {
			String contentType = target.getMediaType();
			if (req.isAcceptable(contentType)) {
				WebObject web = req.getRequestedResource();
				String md5 = web.getContentMD5();
				if (md5 != null) {
					rb.header("Content-MD5", md5);
				}
				String encoding = web.getContentEncoding();
				if (encoding != null) {
					rb.header("Content-Encoding", encoding);
				}
				return rb.file(req.createFileEntity());
			} else {
				return rb.status(406); // Not Acceptable
			}
		} else if (file.exists()) {
			throw new MethodNotAllowed();
		}
		return rb.notFound();
	}

	public Response put(Request req) throws MethodNotAllowed,
			RepositoryException, QueryEvaluationException, IOException,
			NoSuchAlgorithmException {
		ObjectConnection con = req.getObjectConnection();
		String loc = req.getHeader("Content-Location");
		Response rb = new Response().noContent();
		InternalWebObject target = (InternalWebObject) req
				.getRequestedResource();
		if (req.getContentType() == null && loc != null) {
			WebRedirect t = con.addDesignation(target, WebRedirect.class);
			ObjectFactory of = con.getObjectFactory();
			URI uri = req.createURI(loc);
			WebObject to = (WebObject) of.createObject(uri);
			t.setRedirect(to);
			req.flush();
			return rb;
		}
		String contentType = req.getContentType();
		if (contentType != null) {
			target.setMediaType(contentType);
			assert contentType.equals(target.getMediaType());
		}
		File file = req.getFile();
		File bak = null;
		boolean abort = file.exists();
		try {
			InputStream in = req.getInputStream();
			try {
				OutputStream out = target.openOutputStream();
				try {
					try {
						int read;
						byte[] buf = new byte[1024];
						while ((read = in.read(buf)) >= 0) {
							out.write(buf, 0, read);
						}
					} catch (IOException e) {
						return rb.badRequest(e);
					}
					if (abort) {
						File dir = file.getParentFile();
						bak = new File(dir, "$backupof" + file.getName());
						if (bak.exists()) {
							bak.delete();
						}
						if (!file.renameTo(bak)) {
							bak.delete();
							bak = null;
						}
					}
				} finally {
					out.close();
				}
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					return rb.badRequest(e);
				}
			}
			req.flush();
			con.setAutoCommit(true); // prepare()
			abort = false;
			return rb;
		} finally {
			if (abort && bak != null) {
				if (file.exists()) {
					file.delete();
				}
				bak.renameTo(file);
			} else if (bak != null) {
				bak.delete();
			}
		}
	}

	public Response delete(Request req) throws MethodNotAllowed,
			RepositoryException {
		File file = req.getFile();
		if (!file.exists())
			return new Response().notFound();
		if (!file.getParentFile().canWrite())
			throw new MethodNotAllowed();
		WebObject target = req.getRequestedResource();
		if (!target.delete())
			throw new MethodNotAllowed();
		return new Response().noContent();
	}
}
