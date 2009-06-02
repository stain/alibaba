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

import info.aduna.io.MavenUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.result.MultipleResultException;
import org.openrdf.result.NoResultException;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;
import org.openrdf.server.metadata.http.readers.AggregateReader;
import org.openrdf.server.metadata.http.readers.MessageBodyReader;
import org.openrdf.server.metadata.http.writers.AggregateWriter;
import org.openrdf.server.metadata.http.writers.MessageBodyWriter;
import org.openrdf.server.metadata.resources.DeleteResource;
import org.openrdf.server.metadata.resources.GetResource;
import org.openrdf.server.metadata.resources.MetadataResource;
import org.openrdf.server.metadata.resources.OptionsResource;
import org.openrdf.server.metadata.resources.PostResource;
import org.openrdf.server.metadata.resources.PutResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.header.reader.HttpHeaderReader;

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
			try {
				URI uri = getURI(request, con.getValueFactory());
				Response rb;
				try {
					rb = process(uri, request, con);
					try {
						respond(uri, rb, request, response);
					} catch (IOException e) {
						logger.info(e.getMessage(), e);
					}
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
					rb = new Response().server(e);
					try {
						respond(uri, rb, request, response);
					} catch (IOException io) {
						logger.info(io.getMessage(), io);
						response.setStatus(400);
					} catch (ParseException pe) {
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
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			logger.warn(e.getMessage(), e);
			response.setStatus(500);
		}
	}

	private Response process(URI uri, HttpServletRequest request,
			ObjectConnection con) throws Throwable {
		String method = request.getMethod();
		if ("OPTIONS".equals(method) && "*".equals(request.getRequestURI())) {
			return options(request);
		} else if ("TRACE".equals(method)) {
			return trace(request);
		}
		Response rb;
		File file = getFile(request, uri);
		if (unmodifiedSince(request, file.lastModified())) {
			rb = process(method, request, file, uri, con);
			String prefer = request.getHeader("Prefer");
			if (rb.isOk() && "HEAD".equals(method)) {
				rb = rb.head();
			} else if (rb.isOk() && "return-no-content".equals(prefer)) {
				rb = rb.noContent();
			} else if (rb.isNoContent() && "return-content".equals(prefer)) {
				rb = process("GET", request, file, uri, con);
			}
		} else {
			rb = new Response().preconditionFailed();
		}
		return rb;
	}

	private Response process(String method, HttpServletRequest request,
			File file, URI uri, ObjectConnection con) throws Throwable {
		Request req = new Request(reader, writer, request, file, uri, con);
		RDFObject target = getWebResource(uri, con);
		if ("GET".equals(method) || "HEAD".equals(method)) {
			if (modifiedSince(request, file.lastModified())) {
				GetResource resource = new GetResource(file, target);
				Response rb = resource.get(req);
				if (!request.getParameterNames().hasMoreElements()) {
					rb.lastModified(file.lastModified());
				}
				int status = rb.getStatus();
				if (200 <= status && status < 300) {
					rb = addLinks(request, resource, rb);
				}
				return rb;
			}
			return new Response().notModified();
		} else if ("PUT".equals(method)) {
			PutResource resource = new PutResource(file, target);
			return resource.put(req);
		} else if ("DELETE".equals(method)) {
			DeleteResource resource = new DeleteResource(file, target);
			return resource.delete(req);
		} else if ("OPTIONS".equals(method)) {
			OptionsResource resource = new OptionsResource(file, target);
			return addLinks(request, resource, resource.options(req));
		} else {
			PostResource resource = new PostResource(file, target);
			return resource.post(req);
		}
	}

	private RDFObject getWebResource(URI uri, ObjectConnection con)
			throws QueryEvaluationException, NoResultException,
			MultipleResultException, RepositoryException {
		return con.getObject(WebResource.class, uri);
	}

	private File getFile(HttpServletRequest request, URI uri) {
		String host = getHost(request);
		File base = new File(dataDir, safe(host));
		File file = new File(base, safe(getPath(request)));
		if (file.isFile())
			return file;
		int dot = file.getName().lastIndexOf('.');
		String name = Integer.toHexString(uri.hashCode());
		if (dot > 0) {
			name = name + file.getName().substring(dot);
		}
		return new File(file, name);
	}

	private URI getURI(HttpServletRequest request, ValueFactory vf) {
		String uri;
		try {
			String scheme = request.getScheme();
			String host = getHost(request);
			String path = getPath(request);
			uri = new java.net.URI(scheme, host, path, null).toASCIIString();
		} catch (URISyntaxException e) {
			// bad Host header
			StringBuffer url = request.getRequestURL();
			int idx = url.indexOf("?");
			if (idx > 0) {
				uri = url.substring(0, idx);
			} else {
				uri = url.toString();
			}
		}
		return vf.createURI(uri);
	}

	private String getHost(HttpServletRequest request) {
		String host = request.getHeader("Host");
		if (host == null)
			return request.getServerName();
		return host;
	}

	private String getPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		int idx = path.indexOf('?');
		if (idx > 0) {
			path = path.substring(0, idx);
		}
		return path;
	}

	private String safe(String path) {
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace('*', '_');
		path = path.replace('"', '_');
		path = path.replace('[', '_');
		path = path.replace(']', '_');
		path = path.replace(':', '_');
		path = path.replace(';', '_');
		path = path.replace('|', '_');
		path = path.replace('=', '_');
		return path;
	}

	private boolean modifiedSince(HttpServletRequest request, long lastModified) {
		long modified = request.getDateHeader("If-Modified-Since");
		return lastModified <= 0 || modified <= 0 || modified < lastModified;
	}

	private boolean unmodifiedSince(HttpServletRequest request,
			long lastModified) {
		long unmodified = request.getDateHeader("If-Unmodified-Since");
		return unmodified <= 0 || lastModified <= unmodified;
	}

	private Response trace(HttpServletRequest request) {
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

	private Response options(HttpServletRequest request) {
		String allow = "OPTIONS, TRACE, GET, HEAD, PUT, DELETE";
		return new Response().header("Allow", allow);
	}

	private Response addLinks(HttpServletRequest request,
			MetadataResource resource, Response rb) throws RepositoryException {
		String anchor = null;
		if (request.getParameterNames().hasMoreElements()) {
			anchor = "; anchor=<" + getPath(request) + ">";
		}
		for (String link : resource.getLinks()) {
			rb = rb.header("Link", anchor == null ? link : link + anchor);
		}
		return rb;
	}

	private void respond(URI uri, Response rb, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ParseException {
		Object entity = rb.getEntity();
		if (entity == null) {
			headers(rb, response);
		} else if (entity instanceof Throwable) {
			respond(response, rb.getStatus(), (Throwable) entity);
		} else if (entity != null) {
			Class<?> type = entity.getClass();
			MediaType mediaType = null;
			String mimeType = null;
			List<? extends MediaType> acceptable = getAcceptable(request, rb
					.getContentType());
			loop: for (MediaType m : acceptable) {
				String mime = m.getType() + "/" + m.getSubtype();
				if (writer.isWriteable(type, mime)) {
					mediaType = m;
					mimeType = mime;
					break loop;
				}
			}
			if (mimeType == null) {
				notAcceptable(response);
			} else {
				Charset charset = getCharset(mediaType, null);
				respond(uri, rb, response, entity, charset, mimeType);
			}
		}
	}

	private void respond(HttpServletResponse response, int status,
			Throwable entity) throws IOException {
		String msg = entity.getMessage();
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
		String prefix = entity.getClass().getName()+": ";
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

	private void respond(URI uri, Response rb, HttpServletResponse response,
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
				writer.writeTo(entity, uri.stringValue(), mimeType, out,
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

	private Charset getCharset(MediaType m, Charset defCharset) {
		if (m == null)
			return defCharset;
		String name = m.getParameters().get("charset");
		if (name == null)
			return defCharset;
		return Charset.forName(name);
	}

	private List<? extends MediaType> getAcceptable(HttpServletRequest request,
			String accept) throws ParseException {
		StringBuilder sb = new StringBuilder();
		sb.append(accept);
		Enumeration headers = request.getHeaders("Accept");
		while (headers.hasMoreElements()) {
			sb.append(", ");
			sb.append((String) headers.nextElement());
		}
		return HttpHeaderReader.readAcceptMediaType(sb.toString());
	}
}
