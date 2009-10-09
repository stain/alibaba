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
package org.openrdf.server.metadata.controllers;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.expect;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;
import org.openrdf.server.metadata.exceptions.TransformLinkException;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;
import org.openrdf.server.metadata.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicController {
	private static final String ALLOW_HEADERS = "Authorization,Host,Cache-Control,Location,Range,"
			+ "Accept,Accept-Charset,Accept-Encoding,Accept-Language,"
			+ "Content-Encoding,Content-Language,Content-Length,Content-Location,Content-MD5,Content-Type,"
			+ "If-Match,If-Modified-Since,If-None-Match,If-Range,If-Unmodified-Since";
	private Logger logger = LoggerFactory.getLogger(DynamicController.class);
	private FileSystemController fs = new FileSystemController();

	public Operation getOperation(Request req) throws MimeTypeParseException,
			TransformLinkException, RepositoryException,
			QueryEvaluationException {
		String method = req.getMethod();
		if ("GET".equals(method) || "HEAD".equals(method))
			return new Operation(req, fs.existsAndAcceptable(req));
		return new Operation(req, false);
	}

	public Response get(Request req, Operation operation) throws Throwable {
		try {
			Response rb;
			Method method = operation.getMethod();
			if (method != null) {
				rb = invoke(operation, method, req, true);
				if (rb.isNoContent()) {
					rb = new Response().notFound();
				}
			} else {
				rb = fs.get(req);
				if (rb.getStatus() >= 404 && rb.getStatus() <= 406) {
					rb = findAlternate(req, operation, rb);
				}
			}
			if (rb.getHeader("Cache-Control") == null) {
				setCacheControl(req.getRequestedResource().getClass(), rb);
			}
			return rb;
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(operation);
		} catch (BadRequest e) {
			return new Response().exception(e);
		}
	}

	public Response options(Request req, Operation operation)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : operation.getAllowedMethods()) {
			sb.append(", ").append(method);
		}
		String allow = sb.toString();
		Response rb = new Response();
		rb = rb.header("Allow", allow);
		rb = rb.header("Access-Control-Allow-Methods", allow);
		rb = rb.header("Access-Control-Allow-Headers", ALLOW_HEADERS);
		String max = getMaxAge(req.getRequestedResource().getClass());
		if (max != null) {
			rb = rb.header("Access-Control-Max-Age", max);
		}
		return rb;
	}

	public Response post(Request req, Operation operation) throws Throwable {
		try {
			Method method = operation.getMethod();
			if (method == null)
				throw new MethodNotAllowed();
			return invoke(operation, method, req, false);
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(operation);
		} catch (BadRequest e) {
			return new Response().exception(e);
		}
	}

	public Response put(Request req, Operation operation) throws Throwable {
		try {
			Method method = operation.getMethod();
			if (method == null)
				return fs.put(req);
			return invoke(operation, method, req, false);
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(operation);
		} catch (BadRequest e) {
			return new Response().exception(e);
		}
	}

	public Response delete(Request req, Operation operation) throws Throwable {
		try {
			Method method = operation.getMethod();
			if (method == null)
				return fs.delete(req);
			return invoke(operation, method, req, false);
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(operation);
		} catch (BadRequest e) {
			return new Response().exception(e);
		}
	}

	private Response findAlternate(Request req, Operation op, Response rb)
			throws MimeTypeParseException {
		Method operation;
		if ((operation = op.getOperationMethod("alternate")) != null) {
			String loc = req.getURI() + "?"
					+ operation.getAnnotation(operation.class).value()[0];
			return new Response().status(302).location(loc);
		} else if ((operation = op.getOperationMethod("describedby")) != null) {
			String loc = req.getURI() + "?"
					+ operation.getAnnotation(operation.class).value()[0];
			return new Response().status(303).location(loc);
		}
		return rb;
	}

	private Response createResponse(Request req, Method method,
			ResponseEntity entity) throws Exception {
		Response rb = new Response();
		if (method.isAnnotationPresent(cacheControl.class)) {
			for (String value : method.getAnnotation(cacheControl.class)
					.value()) {
				rb.header("Cache-Control", value);
			}
		}
		if (entity.isNoContent()) {
			rb = rb.noContent();
		} else if (entity.isRedirect()) {
			rb = rb.status(307).location(entity.getLocation());
		} else if (entity.isSeeOther()) {
			rb = rb.status(303).location(entity.getLocation());
		} else {
			rb = rb.entity(entity);
		}
		if (method.isAnnotationPresent(expect.class)) {
			String expect = method.getAnnotation(expect.class).value();
			String[] values = expect.split("-");
			try {
				rb.status(Integer.parseInt(values[0]));
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < values.length; i++) {
					sb.append(values[i].substring(0, 1).toUpperCase());
					sb.append(values[i].substring(1));
					sb.append(" ");
				}
				if (sb.length() > 1) {
					rb.status(sb.toString().trim());
				}
			} catch (NumberFormatException e) {
				logger.error(expect, e);
			} catch (IndexOutOfBoundsException e) {
				logger.error(expect, e);
			}
		}
		return rb;
	}

	private Response invoke(Operation operation, Method method, Request req,
			boolean safe) throws Throwable {
		try {
			Object[] args;
			try {
				args = operation.getParameters(method, req.getBody());
			} catch (ParserConfigurationException e) {
				throw e;
			} catch (TransformerConfigurationException e) {
				throw e;
			} catch (Exception e) {
				return new Response().badRequest(e);
			}
			try {
				ObjectConnection con = req.getObjectConnection();
				assert !con.isAutoCommit();
				ResponseEntity entity = operation.invoke(method, args, true);
				if (!safe) {
					req.flush();
				}
				return createResponse(req, method, entity);
			} finally {
				for (Object arg : args) {
					if (arg instanceof Closeable) {
						((Closeable) arg).close();
					}
				}
			}
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private Response methodNotAllowed(Operation operation)
			throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : operation.getAllowedMethods()) {
			sb.append(", ").append(method);
		}
		return new Response().status(405).header("Allow", sb.toString());
	}

	private void setCacheControl(Class<?> type, Response rb) {
		if (type.isAnnotationPresent(cacheControl.class)) {
			for (String value : type.getAnnotation(cacheControl.class).value()) {
				rb.header("Cache-Control", value);
			}
		} else {
			if (type.getSuperclass() != null) {
				setCacheControl(type.getSuperclass(), rb);
			}
			for (Class<?> face : type.getInterfaces()) {
				setCacheControl(face, rb);
			}
		}
	}

	private String getMaxAge(Class<?> type) {
		if (type.isAnnotationPresent(cacheControl.class)) {
			for (String value : type.getAnnotation(cacheControl.class).value()) {
				int m = value.indexOf("max-age=");
				if (m >= 0) {
					int c = value.indexOf(';', m);
					if (c < 0) {
						c = value.length();
					}
					String max = value.substring(m, c);
					return max.trim();
				}
			}
		} else {
			if (type.getSuperclass() != null) {
				String max = getMaxAge(type.getSuperclass());
				if (max != null) {
					return max;
				}
			}
			for (Class<?> face : type.getInterfaces()) {
				String max = getMaxAge(face);
				if (max != null) {
					return max;
				}
			}
		}
		return null;
	}

}
