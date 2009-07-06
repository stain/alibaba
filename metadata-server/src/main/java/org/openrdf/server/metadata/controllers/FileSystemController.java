package org.openrdf.server.metadata.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import javax.activation.MimeTypeParseException;

import org.apache.commons.codec.binary.Base64;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.exceptions.MethodNotAllowedException;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public class FileSystemController {

	public Response get(Request req) throws MethodNotAllowedException,
			RepositoryException, QueryEvaluationException,
			MimeTypeParseException {
		File file = req.getFile();
		RDFResource target = req.getRequestedResource();
		Response rb = new Response(req);
		if (target.getRedirect() != null) {
			String obj = target.getRedirect().getResource().stringValue();
			return rb.status(307).location(obj);
		}
		if (file.canRead()) {
			String contentType = getContentType(req);
			if (req.isAcceptable(contentType)) {
				WebResource web = (WebResource) req.getRequestedResource();
				String md5 = web.getContentMD5();
				if (md5 != null) {
					rb.header("Content-MD5", md5);
				}
				return rb.entity(file);
			} else {
				return rb.status(406); // Not Acceptable
			}
		} else if (file.exists()) {
			throw new MethodNotAllowedException();
		}
		return rb.notFound();
	}

	public Response put(Request req) throws MethodNotAllowedException,
			RepositoryException, QueryEvaluationException, IOException,
			NoSuchAlgorithmException {
		ObjectConnection con = req.getObjectConnection();
		String loc = req.getHeader("Content-Location");
		Response rb = new Response(req).noContent();
		if (req.getContentType() == null && loc != null) {
			ObjectFactory of = con.getObjectFactory();
			URI uri = req.createURI(loc);
			RDFResource redirect = of.createObject(uri, RDFResource.class);
			req.getRequestedResource().setRedirect(redirect);
			req.flush();
			return rb;
		}
		File file = req.getFile();
		File dir = file.getParentFile();
		File tmp = new File(dir, "$partof" + file.getName());
		try {
			dir.mkdirs();
			if (!dir.canWrite())
				throw new MethodNotAllowedException();
			MessageDigest md5 = MessageDigest.getInstance("MD5");
				InputStream in = req.getInputStream();
				try {
					OutputStream out = new FileOutputStream(tmp);
					try {
						byte[] buf = new byte[512];
						int read;
						while ((read = in.read(buf)) >= 0) {
							md5.update(buf, 0, read);
							out.write(buf, 0, read);
						}
					} finally {
						out.close();
					}
				} finally {
					in.close();
				}
			byte[] b = Base64.encodeBase64(md5.digest());
			String digest = new String(b, "UTF-8");
			String contentType = req.getContentType();
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			RDFResource target = req.getRequestedResource();
			target.setRedirect(null);
			WebResource web = setMediaType(target, contentType);
			target = web;
			web.setContentMD5(digest);
			URI uri = (URI) target.getResource();
			con.clear(uri);
			con.setAddContexts(uri);
			web.extractMetadata(tmp);
			req.flush();
			con.setAutoCommit(true); // prepare()
			if (file.exists()) {
				file.delete();
			}
			if (!tmp.renameTo(file)) {
				throw new MethodNotAllowedException();
			}
			rb.lastModified(file.lastModified());
			return rb;
		} catch (FileNotFoundException e) {
			throw new MethodNotAllowedException();
		} catch (IOException e) {
			return rb.badRequest(e);
		} finally {
			tmp.delete();
		}
	}

	public Response delete(Request req) throws MethodNotAllowedException,
			RepositoryException {
		File file = req.getFile();
		if (!file.exists())
			return new Response().notFound();
		if (!file.getParentFile().canWrite())
			throw new MethodNotAllowedException();
		ObjectConnection con = req.getObjectConnection();
		RDFResource target = req.getRequestedResource();
		target.setRedirect(null);
		target.setRevision(null);
		if (target instanceof WebResource) {
			removeMediaType((WebResource) target);
			((WebResource) target).setContentMD5(null);
			con.clear(target.getResource());
		}
		con.setAutoCommit(true); // prepare()
		if (!file.delete())
			throw new MethodNotAllowedException();
		return new Response().noContent();
	}

	private String getContentType(Request req) throws RepositoryException,
			QueryEvaluationException {
		RDFResource target = req.getRequestedResource();
		File file = req.getFile();
		if (target instanceof WebResource
				&& ((WebResource) target).getMediaType() != null)
			return ((WebResource) target).getMediaType();
		String mimeType = getMimeType(file);
		setMediaType(target, mimeType);
		req.flush();
		return mimeType;
	}

	private String getMimeType(File file) {
		Collection types = MimeUtil.getMimeTypes(file);
		MimeType mimeType = null;
		double specificity = 0;
		for (Iterator it = types.iterator(); it.hasNext();) {
			MimeType mt = (MimeType) it.next();
			int spec = mt.getSpecificity() * 2;
			if (!mt.getSubType().startsWith("x-")) {
				spec += 1;
			}
			if (spec > specificity) {
				mimeType = mt;
				specificity = spec;
			}
		}
		if (mimeType == null)
			return "application/octet-stream";
		return mimeType.toString();
	}

	protected WebResource setMediaType(RDFResource target, String mediaType)
			throws RepositoryException {
		ObjectConnection con = target.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		WebResource web;
		if (target instanceof WebResource) {
			web = (WebResource) target;
		} else {
			target = web = con.addDesignation(target, WebResource.class);
		}
		String previous = web.mimeType();
		String next = mediaType;
		web.setMediaType(mediaType);
		if (previous != null) {
			try {
				URI uri = vf.createURI("urn:mimetype:" + previous);
				con.removeDesignations(web, uri);
			} catch (IllegalArgumentException e) {
				// invalid mimetype
			}
		}
		if (next != null) {
			URI uri = vf.createURI("urn:mimetype:" + web.mimeType());
			web = (WebResource) con.addDesignations(web, uri);
		}
		return web;
	}

	protected void removeMediaType(WebResource target)
			throws RepositoryException {
		ObjectConnection con = target.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		if (target != null) {
			String previous = target.mimeType();
			target.setMediaType(null);
			con.removeDesignation(target, WebResource.class);
			if (previous != null) {
				try {
					URI uri = vf.createURI("urn:mimetype:" + previous);
					con.removeDesignations(target, uri);
				} catch (IllegalArgumentException e) {
					// invalid mimetype
				}
			}
		}
	}
}
