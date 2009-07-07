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
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeTypeParseException;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.RDFResource;
import org.openrdf.server.metadata.concepts.WebResource;
import org.openrdf.server.metadata.exceptions.BadRequestException;
import org.openrdf.server.metadata.exceptions.MethodNotAllowedException;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.RequestHeader;
import org.openrdf.server.metadata.http.Response;

public class DynamicController {
	private static final Method IDENTITY_FILE;
	static {
		try {
			IDENTITY_FILE = RequestHeader.class.getDeclaredMethod("getFile");
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}
	private FileSystemController fs = new FileSystemController();

	public List<String> getLinks(Request req) throws RepositoryException {
		Map<String, List<Method>> map = getOperationMethods(req
				.getRequestedResource(), "GET", true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			sb.delete(0, sb.length());
			sb.append("<").append(req.getURI());
			sb.append("?").append(e.getKey()).append(">");
			for (Method m : e.getValue()) {
				if (m.isAnnotationPresent(rel.class)) {
					sb.append("; rel=\"");
					for (String value : m.getAnnotation(rel.class).value()) {
						sb.append(value).append(" ");
					}
					sb.setCharAt(sb.length() - 1, '"');
				}
				if (m.isAnnotationPresent(type.class)) {
					sb.append("; type=\"");
					for (String value : m.getAnnotation(type.class).value()) {
						sb.append(value).append(" ");
					}
					sb.setCharAt(sb.length() - 1, '"');
				}
				if (m.isAnnotationPresent(title.class)) {
					for (String value : m.getAnnotation(title.class).value()) {
						sb.append("; title=\"").append(value).append("\"");
					}
				}
			}
			result.add(sb.toString());
		}
		return result;
	}

	public Class<?> getEntityType(Request req) throws MimeTypeParseException,
			RepositoryException, QueryEvaluationException {
		Method m = getMethod(req);
		if (m == null)
			return null;
		if (IDENTITY_FILE.equals(m))
			return File.class;
		return m.getReturnType();
	}

	public String getContentType(Request req) throws MimeTypeParseException,
			RepositoryException, QueryEvaluationException {
		Method m = getMethod(req);
		if (m == null)
			return null;
		if (IDENTITY_FILE.equals(m))
			return ((WebResource) req.getRequestedResource()).getMediaType();
		if (URL.class.equals(m.getReturnType()))
			return null;
		return req.getContentType(m);
	}

	public String getEntityTag(Request req, String contentType)
			throws MimeTypeParseException, RepositoryException,
			QueryEvaluationException {
		RDFResource target = req.getRequestedResource();
		Method m = getMethod(req);
		String method = req.getMethod();
		if (IDENTITY_FILE.equals(m)) {
			return ((WebResource) req.getRequestedResource()).identityTag();
		} else if (contentType != null) {
			return target.variantTag(contentType);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			if (m != null)
				return target.variantTag(null);
			Response rb = fs.get(req);
			if (rb.getStatus() >= 404 && rb.getStatus() <= 406) {
				Method operation;
				if ((operation = getOperationMethod("alternate", req)) != null) {
					return target.variantTag(req.getContentType(operation));
				} else if ((operation = getOperationMethod("describedby", req)) != null) {
					return target.variantTag(req.getContentType(operation));
				}
			}
		} else if ("PUT".equals(method)) {
			if (target instanceof WebResource) {
				String media = ((WebResource) target).getMediaType();
				if (media != null && media.equals(req.getContentType())) {
					return ((WebResource) target).identityTag();
				}
			}
			return target.variantTag(req.getContentType());
		} else if ("DELETE".equals(method) || "OPTIONS".equals(method)) {
			return target.variantTag(null);
		}
		return null;
	}

	public Response get(Request req) throws Throwable {
		try {
			Response rb;
			Method method = findMethod(req, true);
			if (method != null) {
				rb = invoke(method, req, true);
				if (rb.isNoContent()) {
					rb = new Response(req).notFound();
				}
			} else {
				rb = fs.get(req);
				if (rb.getStatus() >= 404 && rb.getStatus() <= 406) {
					rb = findAlternate(req, rb);
				}
			}
			if (rb.getHeader("Cache-Control") == null) {
				setCacheControl(req.getRequestedResource().getClass(), rb);
			}
			return rb;
		} catch (MethodNotAllowedException e) {
			return methodNotAllowed(req);
		} catch (BadRequestException e) {
			return new Response(req).badRequest();
		}
	}

	private Response findAlternate(Request req, Response rb)
			throws MimeTypeParseException {
		Method operation;
		if ((operation = getOperationMethod("alternate", req)) != null) {
			String loc = req.getURI() + "?"
					+ operation.getAnnotation(operation.class).value()[0];
			return new Response(req).status(302).location(loc);
		} else if ((operation = getOperationMethod("describedby", req)) != null) {
			String loc = req.getURI() + "?"
					+ operation.getAnnotation(operation.class).value()[0];
			return new Response(req).status(303).location(loc);
		}
		return rb;
	}

	public Response options(Request req) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response(req).header("Allow", sb.toString());
	}

	public Response post(Request req) throws Throwable {
		try {
			Method method = findMethod(req);
			if (method == null)
				throw new MethodNotAllowedException();
			return invoke(method, req, false);
		} catch (MethodNotAllowedException e) {
			return methodNotAllowed(req);
		} catch (BadRequestException e) {
			return new Response(req).badRequest();
		}
	}

	public Response put(Request req) throws Throwable {
		try {
			Method method = findMethod(req, false);
			if (method == null)
				return fs.put(req);
			return invoke(method, req, false);
		} catch (MethodNotAllowedException e) {
			return methodNotAllowed(req);
		} catch (BadRequestException e) {
			return new Response(req).badRequest();
		}
	}

	public Response delete(Request req) throws Throwable {
		try {
			Method method = findMethod(req, false);
			if (method == null)
				return fs.delete(req);
			return invoke(method, req, false);
		} catch (MethodNotAllowedException e) {
			return methodNotAllowed(req);
		} catch (BadRequestException e) {
			return new Response(req).badRequest();
		}
	}

	private Response createResponse(Request req, Method method, Object entity) {
		if (entity instanceof RDFObjectBehaviour) {
			entity = ((RDFObjectBehaviour) entity).getBehaviourDelegate();
		}
		Response rb = new Response(req);
		if (method.isAnnotationPresent(cacheControl.class)) {
			for (String value : method.getAnnotation(cacheControl.class)
					.value()) {
				rb.header("Cache-Control", value);
			}
		}
		RDFResource target = req.getRequestedResource();
		Class<?> type = method.getReturnType();
		if (entity != null && URL.class.equals(type)) {
			return rb.status(307).location(entity.toString());
		} else if (type.equals(Set.class)) {
			Set set = (Set) entity;
			Iterator iter = set.iterator();
			try {
				if (!iter.hasNext())
					return rb.notFound();
				entity = iter.next();
				if (iter.hasNext()) {
					return rb.entity(set);
				}
			} finally {
				target.getObjectConnection().close(iter);
			}
		}
		if (entity instanceof RDFResource && !target.equals(entity)) {
			RDFResource rdf = (RDFResource) entity;
			Resource resource = rdf.getResource();
			if (resource instanceof URI) {
				URI uri = (URI) resource;
				return rb.status(303).location(uri.stringValue());
			}
		}
		return rb.entity(entity);
	}

	private Method findBestMethod(Request req, List<Method> methods)
			throws MimeTypeParseException {
		Method best = null;
		loop: for (Method method : methods) {
			Class<?>[] ptypes = method.getParameterTypes();
			Annotation[][] anns = method.getParameterAnnotations();
			Type[] gtypes = method.getGenericParameterTypes();
			Object[] args = new Object[ptypes.length];
			// TODO if no req body then prefer methods that have no parameter
			for (int i = 0; i < args.length; i++) {
				String[] names = getParameterNames(anns[i]);
				if (names == null && !ptypes[i].equals(File.class)) {
					if (!req.isReadable(ptypes[i], gtypes[i]))
						continue loop;
				}
			}
			best = method;
			Class<?> type = method.getReturnType();
			if (!type.equals(Void.TYPE)) {
				if (method.isAnnotationPresent(type.class)) {
					for (String media : method.getAnnotation(type.class)
							.value()) {
						if (!req.isAcceptable(media, type))
							continue;
						return best;
					}
					continue loop;
				} else {
					if (!req.isAcceptable(type))
						continue loop;
				}
			}
			return best;
		}
		return best;
	}

	private Method findMethod(Request req) throws MimeTypeParseException {
		return findMethod(req, null);
	}

	private Method findMethod(Request req, Boolean isResponsePresent)
			throws MimeTypeParseException {
		Method method = null;
		boolean isMethodPresent = false;
		String name = req.getOperation();
		RDFResource target = req.getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(target, req.getMethod(),
					isResponsePresent).get(name);
			if (methods != null) {
				isMethodPresent = true;
				method = findBestMethod(req, methods);
			}
		}
		if (method == null) {
			List<Method> methods = getPostMethods(target).get(req.getMethod());
			if (methods != null) {
				method = findBestMethod(req, methods);
				if (method == null)
					throw new BadRequestException();
			}
		}
		if (method == null) {
			if (isMethodPresent)
				throw new BadRequestException();
			if (req.isQueryStringPresent())
				throw new MethodNotAllowedException();
		}
		return method;
	}

	private Map<String, List<Method>> getOperationMethods(RDFResource target,
			String method, Boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			if (isRespBody != null
					&& isRespBody != !m.getReturnType().equals(Void.TYPE))
				continue;
			operation ann = m.getAnnotation(operation.class);
			if (ann == null)
				continue;
			if (m.isAnnotationPresent(method.class)) {
				for (String v : m.getAnnotation(method.class).value()) {
					if (method.equals(v)) {
						put(map, ann.value(), m);
						break;
					}
				}
			} else if ("GET".equals(method) || "HEAD".equals(method)
					|| "PUT".equals(method) || "DELETE".equals(method)) {
				// don't require method annotation for these methods
				put(map, ann.value(), m);
			}
		}
		return map;
	}

	private Map<String, List<Method>> getPostMethods(RDFResource target) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			method ann = m.getAnnotation(method.class);
			if (ann == null)
				continue;
			put(map, ann.value(), m);
		}
		return map;
	}

	private Set<String> getAllowedMethods(Request req)
			throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = req.getOperation();
		File file = req.getFile();
		RDFResource target = req.getRequestedResource();
		if (!req.isQueryStringPresent() && file.canRead()
				|| getOperationMethods(target, "GET", true).containsKey(name)) {
			set.add("GET");
			set.add("HEAD");
		}
		if (!req.isQueryStringPresent()) {
			if (!file.exists() || file.canWrite()) {
				set.add("PUT");
			}
			if (file.exists() && file.getParentFile().canWrite()) {
				set.add("DELETE");
			}
		} else if (getOperationMethods(target, "PUT", false).containsKey(name)) {
			set.add("PUT");
		} else if (getOperationMethods(target, "DELETE", false).containsKey(
				name)) {
			set.add("DELETE");
		}
		Map<String, List<Method>> map = getPostMethods(target);
		for (String method : map.keySet()) {
			set.add(method);
		}
		return set;
	}

	private Method getOperationMethod(String rel, Request req)
			throws MimeTypeParseException {
		Map<String, List<Method>> map = getOperationMethods(req
				.getRequestedResource(), "GET", true);
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			for (Method m : e.getValue()) {
				if (m.isAnnotationPresent(rel.class)) {
					for (String value : m.getAnnotation(rel.class).value()) {
						if (rel.equals(value) && req.isAcceptable(m)) {
							return m;
						}
					}
				}
			}
		}
		return null;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] instanceof parameter)
				return ((parameter) annotations[i]).value();
		}
		return null;
	}

	private Object[] getParameters(Method method, Request req)
			throws RepositoryException, IOException, MimeTypeParseException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names == null && ptypes[i].equals(File.class)) {
				args[i] = req.getFile();
			} else if (names == null) {
				args[i] = req.getBody(ptypes[i], gtypes[i]);
			} else if (names.length == 1 && names[0].equals("*")
					&& ptypes[i].isAssignableFrom(Map.class)) {
				args[i] = req.getParameterMap();
			} else {
				args[i] = req.getParameter(names, gtypes[i], ptypes[i]);
			}
		}
		return args;
	}

	private Response invoke(Method method, Request req, boolean safe)
			throws Throwable {
		try {
			Object[] args;
			try {
				args = getParameters(method, req);
			} catch (Exception e) {
				return new Response(req).badRequest(e);
			}
			try {
				ObjectConnection con = req.getObjectConnection();
				assert !con.isAutoCommit();
				Object entity = method.invoke(req.getRequestedResource(), args);
				if (safe) {
					con.rollback();
				} else {
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

	private Response methodNotAllowed(Request req) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response(req).status(405).header("Allow", sb.toString());
	}

	private void put(Map<String, List<Method>> map, String[] keys, Method m) {
		for (String key : keys) {
			List<Method> list = map.get(key);
			if (list == null) {
				map.put(key, list = new ArrayList<Method>());
			}
			list.add(m);
		}
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

	public Method getMethod(Request req) throws MimeTypeParseException,
			RepositoryException, QueryEvaluationException {
		String method = req.getMethod();
		if ("GET".equals(method) || "HEAD".equals(method)) {
			try {
				Method m = findMethod(req, true);
				if (m != null)
					return m;
				Response rb = fs.get(req);
				if (rb.getStatus() >= 404 && rb.getStatus() <= 406)
					return null;
				if (rb.getEntity() != null)
					return IDENTITY_FILE;
				return null;
			} catch (MethodNotAllowedException e) {
				return null;
			} catch (BadRequestException e) {
				return null;
			}
		} else if ("PUT".equals(method) || "DELETE".equals(method)
				|| "OPTIONS".equals(method)) {
			return null;
		} else {
			try {
				Method m = findMethod(req);
				if (m == null)
					return null;
				return m;
			} catch (MethodNotAllowedException e) {
				return null;
			} catch (BadRequestException e) {
				return null;
			}
		}
	}

}
