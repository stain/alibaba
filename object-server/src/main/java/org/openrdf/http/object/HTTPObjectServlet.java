/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object;

import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.controllers.DynamicController;
import org.openrdf.http.object.controllers.Operation;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.http.Request;
import org.openrdf.http.object.http.Response;
import org.openrdf.http.object.locks.FileLockManager;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the response for requests using the {@link Servlet} API.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectServlet extends GenericServlet {
	private Logger logger = LoggerFactory.getLogger(HTTPObjectServlet.class);
	private ObjectRepository repository;
	private File dataDir;
	private FileLockManager locks = new FileLockManager();
	private DynamicController controller = new DynamicController();
	private final String passwd;

	public HTTPObjectServlet(ObjectRepository repository, File dataDir,
			String passwd) {
		this.repository = repository;
		this.dataDir = dataDir;
		this.passwd = passwd;
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		try {
			ObjectConnection con = repository.getConnection();
			Request r = new Request(dataDir, request, con);
			try {
				Lock lock = createFileLock(request.getMethod(), r.getFile());
				try {
					service(r, response);
				} finally {
					if (lock != null) {
						lock.release();
					}
				}
			} finally {
				con.close();
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(503);
		} catch (QueryEvaluationException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(500);
		} catch (RepositoryException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(500);
		}
	}

	private Lock createFileLock(String method, File file)
			throws InterruptedException {
		if (!method.equals("PUT") && !file.exists())
			return null;
		boolean shared = method.equals("GET") || method.equals("HEAD")
				|| method.equals("OPTIONS") || method.equals("TRACE")
				|| method.equals("POST") || method.equals("PROPFIND");
		return locks.lock(file, shared);
	}

	private void service(Request req, HttpServletResponse response) {
		Response rb;
		try {
			rb = process(req);
			try {
				respond(req, rb, response);
			} catch (IOException e) {
				logger.debug(e.getMessage(), e);
			}
		} catch (ConcurrencyException e) {
			logger.info(e.getMessage(), e);
			rb = new Response().conflict(e);
			try {
				respond(req, rb, response);
			} catch (IOException io) {
				logger.debug(io.getMessage(), io);
				response.setStatus(400);
			} catch (MimeTypeParseException pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(400);
			} catch (Exception pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(500);
			}
		} catch (MimeTypeParseException e) {
			response.setStatus(406);
		} catch (ResponseException e) {
			logger.debug(e.toString(), e);
			rb = new Response().exception(e);
			try {
				respond(req, rb, response);
			} catch (IOException io) {
				logger.debug(io.getMessage(), io);
				response.setStatus(400);
			} catch (MimeTypeParseException pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(400);
			} catch (Exception pe) {
				logger.error(pe.getMessage(), pe);
				response.setStatus(500);
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			rb = new Response().server(e);
			try {
				respond(req, rb, response);
			} catch (IOException io) {
				logger.debug(io.getMessage(), io);
				response.setStatus(400);
			} catch (MimeTypeParseException pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(400);
			} catch (Exception pe) {
				logger.error(pe.getMessage(), pe);
				response.setStatus(500);
			}
		} catch (Error e) {
			logger.error(e.getMessage(), e);
			throw e;
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			response.setStatus(500);
		}
	}

	private Response process(Request request) throws Throwable {
		Response rb;
		String method = request.getMethod();
		ObjectConnection con = request.getObjectConnection();
		con.setAutoCommit(false); // begin()
		Operation operation = controller.getOperation(request);
		if (operation.isAuthenticating()
				&& !isRoot(method, request.getHeader("Authorization"))) {
			if (!operation.isAuthorized()) {
				InputStream message = operation.unauthorized();
				return new Response().unauthorized(message);
			}
		}
		Class<?> type = operation.getEntityType();
		String contentType = operation.getContentType();
		String contentEncoding = operation.getContentEncoding();
		String entityTag = operation.getEntityTag(contentType);
		long lastModified = operation.getLastModified();
		String cache = operation.getCacheControl();
		if (request.unmodifiedSince(entityTag, lastModified)) {
			rb = process(operation, request, entityTag, lastModified);
			if (rb.isOk() && "HEAD".equals(method)) {
				rb = rb.head();
			}
		} else {
			rb = new Response().preconditionFailed();
		}
		if (request.isSafe()) {
			request.rollback();
		} else {
			entityTag = operation.getEntityTag(contentType);
			lastModified = operation.getLastModified();
			request.commit();
		}
		if (cache != null) {
			rb.header("Cache-Control", cache);
		}
		if (operation.isVaryOrigin()) {
			rb.header("Vary", "Origin");
		}
		for (String vary : request.getVary()) {
			rb.header("Vary", vary);
		}
		if (entityTag != null) {
			rb.header("ETag", entityTag);
		}
		if (contentType != null && rb.getEntity() != null) {
			rb.header("Content-Type", contentType);
		}
		if (contentEncoding != null && rb.getEntity() != null) {
			rb.header("Content-Encoding", contentEncoding);
		}
		if (lastModified > 0) {
			rb.lastModified(lastModified);
		}
		if ("GET".equals(method) || "HEAD".equals(method)
				|| "POST".equals(method) || "OPTIONS".equals(method)) {
			String origins = operation.allowOrigin();
			if (origins == null || origins.length() < 1) {
				rb = rb.header("Access-Control-Allow-Origin", "*");
			} else {
				rb = rb.header("Access-Control-Allow-Origin", origins);
			}
		}
		rb.setEntityType(type);
		return rb;
	}

	private Response process(Operation operation, Request req,
			String entityTag, long lastModified) throws Throwable {
		String method = req.getMethod();
		if ("GET".equals(method) || "HEAD".equals(method)) {
			if (req.modifiedSince(entityTag, lastModified)) {
				Response rb = controller.get(req, operation);
				int status = rb.getStatus();
				if (200 <= status && status < 300) {
					rb = addLinks(req, operation, rb);
				}
				return rb;
			}
			return new Response().notModified();
		} else if (req.modifiedSince(entityTag, lastModified)) {
			if ("OPTIONS".equals(method)) {
				Response rb = controller.options(req, operation);
				return addLinks(req, operation, rb);
			} else {
				return controller.post(req, operation);
			}
		} else {
			return new Response().preconditionFailed();
		}
	}

	private boolean isRoot(String method, String auth)
			throws UnsupportedEncodingException {
		if (passwd == null || auth == null)
			return false;
		if (auth.startsWith("Basic")) {
			byte[] bytes = ("root:" + passwd).getBytes("UTF-8");
			String encoded = new String(Base64.encodeBase64(bytes));
			return auth.equals("Basic " + encoded);
		} else if (auth.startsWith("Digest")) {
			String string = auth.substring("Digest ".length());
			Map<String, String> options = parseOptions(string);
			if (options == null)
				return false;
			if (!"root".equals(options.get("username")))
				return false;
			String realm = options.get("realm");
			String a1 = "root:" + realm + ":" + passwd;
			String a2 = method + ":" + options.get("uri");
			String legacy = md5(a1) + ":" + options.get("nonce") + ":"
					+ md5(a2);
			if (md5(legacy).equals(options.get("response")))
				return true;
			String response = md5(a1) + ":" + options.get("nonce") + ":"
					+ options.get("nc") + ":" + options.get("cnonce") + ":"
					+ options.get("qop") + ":" + md5(a2);
			return md5(response).equals(options.get("response"));
		}
		return false;
	}

	private String md5(String a2) {
		return DigestUtils.md5Hex(a2);
	}

	private Map<String, String> parseOptions(String options) {
		Map<String, String> result = new HashMap<String, String>();
		for (String keyvalue : options.split("\\s*,\\s*")) {
			int idx = keyvalue.indexOf('=');
			if (idx < 0)
				return null;
			String key = keyvalue.substring(0, idx);
			if (keyvalue.charAt(idx + 1) == '"') {
				int eq = keyvalue.lastIndexOf('"');
				if (eq <= idx + 2)
					return null;
				String value = keyvalue.substring(idx + 2, eq);
				result.put(key, value);
			} else {
				String value = keyvalue.substring(idx + 1);
				result.put(key, value);
			}
		}
		return result;
	}

	private Response addLinks(Request request, Operation operation, Response rb)
			throws RepositoryException {
		if (!request.isQueryStringPresent()) {
			for (String link : operation.getLinks()) {
				rb = rb.header("Link", link);
			}
		}
		return rb;
	}

	private void respond(Request req, Response rb, HttpServletResponse response)
			throws IOException, MimeTypeParseException, RepositoryException,
			QueryEvaluationException {
		if (!rb.isContent()) {
			headers(rb, response);
		} else if (rb.isException()) {
			respond(response, rb.getStatus(), rb.getMessage(), rb
					.getException());
		} else {
			respond(req, rb, rb, response);
		}
	}

	private void respond(Request req, Response entity, Response rb,
			HttpServletResponse response) throws MimeTypeParseException,
			RepositoryException, QueryEvaluationException, IOException {
		String contentType = rb.getHeader("Content-Type");
		if (contentType == null) {
			notAcceptable(response);
		} else {
			MimeType mediaType = new MimeType(contentType);
			String mimeType = mediaType.getPrimaryType() + "/"
					+ mediaType.getSubType();
			Charset charset = getCharset(mediaType);
			headers(rb, response);
			response.setContentType(contentType);
			long size = entity.getSize(mimeType, charset);
			if (size >= 0) {
				response.addHeader("Content-Length", String.valueOf(size));
			}
			if (!rb.isHead()) {
				OutputStream out = response.getOutputStream();
				try {
					entity.writeTo(mimeType, charset, out, response
							.getBufferSize());
				} catch (OpenRDFException e) {
					logger.warn(e.getMessage(), e);
				} catch (XMLStreamException e) {
					logger.warn(e.getMessage(), e);
				} catch (TransformerException e) {
					logger.warn(e.getMessage(), e);
				} catch (ParserConfigurationException e) {
					logger.warn(e.getMessage(), e);
				} finally {
					out.close();
				}
			}
		}
	}

	private void respond(HttpServletResponse response, int status, String msg,
			ResponseException entity) throws IOException {
		response.setStatus(status, msg);
		response.setHeader("Content-Type", "text/plain;charset=UTF-8");
		response.setDateHeader("Date", System.currentTimeMillis());
		ServletOutputStream out = response.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		PrintWriter print = new PrintWriter(writer);
		try {
			entity.printTo(print);
		} finally {
			print.close();
		}
	}

	private void notAcceptable(HttpServletResponse response) {
		response.setStatus(406);
	}

	private void headers(Response rb, HttpServletResponse response)
			throws MimeTypeParseException, IOException {
		String msg = rb.getMessage();
		if (msg == null) {
			response.setStatus(rb.getStatus());
		} else {
			response.setStatus(rb.getStatus(), msg);
		}
		response.setDateHeader("Date", System.currentTimeMillis());
		for (String header : rb.getHeaderNames()) {
			response.addHeader(header, rb.getHeader(header));
		}
		long value = rb.getLastModified();
		if (value > 0) {
			response.setDateHeader("Last-Modified", value);
		}
	}

	private Charset getCharset(MimeType m) {
		if (m == null)
			return null;
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}
}