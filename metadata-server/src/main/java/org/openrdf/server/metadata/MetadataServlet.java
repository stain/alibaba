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
package org.openrdf.server.metadata;

import info.aduna.concurrent.locks.Lock;
import info.aduna.io.MavenUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.OpenRDFException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.controllers.Controller;
import org.openrdf.server.metadata.controllers.DeleteController;
import org.openrdf.server.metadata.controllers.GetController;
import org.openrdf.server.metadata.controllers.OptionsController;
import org.openrdf.server.metadata.controllers.PostController;
import org.openrdf.server.metadata.controllers.PutController;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;
import org.openrdf.server.metadata.http.readers.AggregateReader;
import org.openrdf.server.metadata.http.readers.MessageBodyReader;
import org.openrdf.server.metadata.http.writers.AggregateWriter;
import org.openrdf.server.metadata.http.writers.MessageBodyWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the response for requests using the {@link Servlet} API.
 * 
 * @author James Leigh
 * 
 */
public class MetadataServlet extends GenericServlet {
	private static final String VERSION = MavenUtil.loadVersion(
			"org.openrdf.alibaba", "alibaba-server-metadata", "devel");
	private static final String APP_NAME = "OpenRDF AliBaba metadata-server";
	protected static final String DEFAULT_NAME = APP_NAME + "/" + VERSION;

	private Logger logger = LoggerFactory.getLogger(MetadataServlet.class);
	private String name = DEFAULT_NAME;
	private ObjectRepository repository;
	private File dataDir;
	private MessageBodyWriter writer = new AggregateWriter();
	private MessageBodyReader reader = new AggregateReader();

	public MetadataServlet(ObjectRepository repository, File dataDir) {
		this.repository = repository;
		this.dataDir = dataDir;
	}

	public String getServerName() {
		return name;
	}

	public void setServerName(String serverName) {
		this.name = serverName;
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		try {
			ObjectConnection con = repository.getConnection();
			Request r = new Request(reader, writer, dataDir, request, con);
			try {
				String method = request.getMethod();
				File file = r.getFile();
				boolean locking = method.equals("PUT") || file.exists();
				Lock lock = locking ? createFileLock(method, file) : null;
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
		} catch (QueryEvaluationException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(500);
		} catch (RepositoryException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(500);
		}
	}

	private Lock createFileLock(String method, File file) throws IOException {
		File parent = file.getParentFile();
		parent.mkdirs();
		File locker = new File(parent, "$lock");
		try {
			final FileChannel channel = new RandomAccessFile(locker, "rw")
					.getChannel();
			try {
				boolean shared = !method.equals("PUT")
						&& !method.equals("DELETE");
				final FileLock lock = channel.lock(0, Long.MAX_VALUE, shared);
				return new Lock() {
					private boolean released;

					public boolean isActive() {
						return !released;
					}

					public void release() {
						try {
							released = true;
							lock.release();
							channel.close();
						} catch (IOException e) {
							logger.warn(e.getMessage(), e);
						}
					}
				};
			} catch (IOException e) {
				channel.close();
				throw e;
			} catch (RuntimeException e) {
				channel.close();
				throw e;
			}
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	private void service(Request req,
			HttpServletResponse response) {
		Response rb;
		try {
			rb = process(req);
			try {
				respond(req, rb, response);
			} catch (IOException e) {
				logger.info(e.getMessage(), e);
			}
		} catch (ConcurrencyException e) {
			logger.info(e.getMessage(), e);
			rb = new Response().conflict(e);
			try {
				respond(req, rb, response);
			} catch (IOException io) {
				logger.info(io.getMessage(), io);
				response.setStatus(400);
			} catch (MimeTypeParseException pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(400);
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			rb = new Response().server(e);
			try {
				respond(req, rb, response);
			} catch (IOException io) {
				logger.info(io.getMessage(), io);
				response.setStatus(400);
			} catch (MimeTypeParseException pe) {
				logger.info(pe.getMessage(), pe);
				response.setStatus(400);
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
		String method = request.getMethod();
		if ("OPTIONS".equals(method) && "*".equals(request.getRequestURI())) {
			return options(request);
		} else if ("TRACE".equals(method)) {
			return trace(request);
		}
		Response rb;
		ObjectConnection con = request.getObjectConnection();
		con.setAutoCommit(false); // begin()
		if (request.unmodifiedSince()) {
			rb = process(method, request);
			String prefer = request.getHeader("Prefer");
			if (rb.isOk() && "HEAD".equals(method)) {
				rb = rb.head();
			} else if (rb.isOk() && "return-no-content".equals(prefer)) {
				rb = rb.noContent();
			} else if (rb.isNoContent() && "return-content".equals(prefer)) {
				rb = process("GET", request);
			}
		} else {
			rb = new Response().preconditionFailed();
		}
		con.setAutoCommit(true); // commit()
		return rb;
	}

	private Response process(String method, Request req) throws Throwable {
		File file = req.getFile();
		RDFResource target = req.getRequestedResource();
		if ("GET".equals(method) || "HEAD".equals(method)) {
			if (req.modifiedSince()) {
				GetController controller = new GetController(file, target);
				Response rb = controller.get(req);
				int status = rb.getStatus();
				if (200 <= status && status < 300) {
					rb = addLinks(req, controller, rb);
				}
				return rb;
			}
			return new Response().notModified();
		} else if (req.modifiedSince()) {
			if ("PUT".equals(method)) {
				return new PutController(file, target).put(req);
			} else if ("DELETE".equals(method)) {
				return new DeleteController(file, target).delete(req);
			} else if ("OPTIONS".equals(method)) {
				OptionsController controller = new OptionsController(file, target);
				return addLinks(req, controller, controller.options(req));
			} else {
				return new PostController(file, target).post(req);
			}
		} else {
			return new Response().preconditionFailed();
		}
	}

	private Response trace(Request request) {
		StringBuilder sb = new StringBuilder();
		sb.append("TRACE ").append(request.getRequestURI()).append("\r\n");
		Enumeration headers = request.getHeaderNames();
		while (headers.hasMoreElements()) {
			String header = (String) headers.nextElement();
			Enumeration values = request.getHeaders(header);
			while (values.hasMoreElements()) {
				String value = (String) values.nextElement();
				sb.append(header).append(": ");
				sb.append(value).append("\r\n");
			}
		}
		sb.append("\r\n");
		return new Response().type("message/http").entity(sb.toString());
	}

	private Response options(Request request) {
		String allow = "OPTIONS, TRACE, GET, HEAD, PUT, DELETE";
		return new Response().header("Allow", allow);
	}

	private Response addLinks(Request request,
			Controller controller, Response rb) throws RepositoryException {
		if (!request.isQueryStringPresent()) {
			for (String link : controller.getLinks()) {
				rb = rb.header("Link", link);
			}
		}
		return rb;
	}

	private void respond(Request req, Response rb, HttpServletResponse response) throws IOException, MimeTypeParseException {
		Object entity = rb.getEntity();
		if (entity == null) {
			headers(rb, response);
		} else if (entity instanceof Throwable) {
			respond(response, rb.getStatus(), (Throwable) entity);
		} else if (entity != null) {
			Class<?> type = entity.getClass();
			MimeType mediaType = null;
			String mimeType = null;
			Collection<? extends MimeType> acceptable = req.getAcceptable(rb
					.getContentType());
			loop: for (MimeType m : acceptable) {
				String mime = m.getPrimaryType() + "/" + m.getSubType();
				if (writer.isWriteable(type, mime)) {
					mediaType = m;
					mimeType = mime;
					break loop;
				}
			}
			if (mimeType == null) {
				notAcceptable(response);
			} else {
				Charset charset = getCharset(mediaType);
				String uri = req.getURI();
				respond(uri, rb, response, entity, charset, mimeType);
			}
		}
	}

	private void respond(HttpServletResponse response, int status,
			Throwable entity) throws IOException {
		String msg = entity.getMessage();
		if (msg == null) {
			msg = entity.toString();
		}
		if (msg.contains("\r")) {
			msg = msg.substring(0, msg.indexOf('\r'));
		}
		if (msg.contains("\n")) {
			msg = msg.substring(0, msg.indexOf('\n'));
		}
		msg = trimPrefix(msg, entity);
		response.setStatus(status, msg);
		if (name != null) {
			response.setHeader("Server", name);
		}
		response.setHeader("Content-Type", "text/plain");
		entity.printStackTrace(response.getWriter());
	}

	private String trimPrefix(String msg, Throwable entity) {
		String prefix = entity.getClass().getName() + ": ";
		if (msg.startsWith(prefix)) {
			msg = msg.substring(prefix.length());
		}
		if (entity.getCause() == null)
			return msg;
		return trimPrefix(msg, entity.getCause());
	}

	private void notAcceptable(HttpServletResponse response) {
		response.setStatus(406);
		if (name != null) {
			response.setHeader("Server", name);
		}
	}

	private void respond(String uri, Response rb, HttpServletResponse response,
			Object entity, Charset charset, String mimeType) throws IOException {
		headers(rb, response);
		long size = writer.getSize(entity, mimeType);
		if (size >= 0) {
			response.addHeader("Content-Length", String.valueOf(size));
		}
		String contentType = writer.getContentType(entity.getClass(), mimeType,
				charset);
		response.addHeader("Content-Type", contentType);
		if (!rb.isHead()) {
			OutputStream out = response.getOutputStream();
			try {
				writer.writeTo(entity, uri, mimeType, out,
						charset);
			} catch (OpenRDFException e) {
				logger.warn(e.getMessage(), e);
			} finally {
				out.close();
			}
		}
	}

	private void headers(Response rb, HttpServletResponse response) {
		response.setStatus(rb.getStatus());
		if (name != null) {
			response.setHeader("Server", name);
		}
		for (String header : rb.getHeaderNames()) {
			for (String value : rb.getHeaders(header)) {
				response.addHeader(header, value);
			}
			Long value = rb.getDateHeader(header);
			if (value != null) {
				response.setDateHeader(header, value);
			}
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
