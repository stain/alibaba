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

import info.aduna.net.ParsedURI;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
import org.openrdf.model.ValueFactory;
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
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.Response;

/**
 * Base class for request handlers.
 * 
 * @author James Leigh
 *
 */
public class Controller {
	private File file;
	protected RDFResource target;

	public Controller(File file, RDFResource target) {
		this.file = file;
		this.target = target;
	}

	public File getFile() {
		return file;
	}

	public WebResource getWebResource() {
		if (target instanceof WebResource)
			return (WebResource) target;
		return null;
	}

	public Object getTarget() {
		return target;
	}

	public URI getURI() {
		return (URI) target.getResource();
	}

	public ObjectConnection getObjectConnection() {
		return target.getObjectConnection();
	}

	public URI createURI(String uriSpec) {
		ParsedURI base = new ParsedURI(getURI().stringValue());
		base.normalize();
		ParsedURI uri = new ParsedURI(uriSpec);
		ValueFactory vf = target.getObjectConnection().getValueFactory();
		return vf.createURI(base.resolve(uri).toString());
	}

	public List<String> getLinks() throws RepositoryException {
		Map<String, List<Method>> map = getOperationMethods(true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			sb.delete(0, sb.length());
			sb.append("<").append(target.getResource().stringValue());
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

	protected String getOperationName(String rel, Request req)
			throws MimeTypeParseException {
		Map<String, List<Method>> map = getOperationMethods(true);
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			for (Method m : e.getValue()) {
				if (m.isAnnotationPresent(rel.class)) {
					for (String value : m.getAnnotation(rel.class).value()) {
						if (rel.equals(value)) {
							if (m.isAnnotationPresent(type.class)) {
								for (String media : m.getAnnotation(type.class)
										.value()) {
									if (req.isAcceptable(media))
										return e.getKey();
								}
							} else {
								return e.getKey();
							}
						}
					}
				}
			}
		}
		return null;
	}

	protected Response methodNotAllowed(Request req) throws RepositoryException {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS, TRACE");
		for (String method : getAllowedMethods(req)) {
			sb.append(", ").append(method);
		}
		return new Response().status(405).header("Allow", sb.toString());
	}

	protected Set<String> getAllowedMethods(Request req)
			throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = req.getOperation();
		if (!req.isQueryStringPresent() && file.canRead()
				|| getOperationMethods(true).containsKey(name)) {
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
		} else if (getOperationMethods(false).containsKey(name)) {
			set.add("PUT");
			set.add("DELETE");
		}
		Map<String, List<Method>> map = getPostMethods();
		for (String method : map.keySet()) {
			set.add(method);
		}
		return set;
	}

	protected Map<String, List<Method>> getOperationMethods(boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			if (isRespBody != !m.getReturnType().equals(Void.TYPE))
				continue;
			operation ann = m.getAnnotation(operation.class);
			if (ann == null)
				continue;
			put(map, ann.value(), m);
		}
		return map;
	}

	protected Map<String, List<Method>> getPostMethods() {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			method ann = m.getAnnotation(method.class);
			if (ann == null)
				continue;
			put(map, ann.value(), m);
		}
		return map;
	}

	protected WebResource setMediaType(String mediaType)
			throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		WebResource target = getWebResource();
		if (target == null) {
			target = con.addDesignation(getTarget(), WebResource.class);
		}
		String previous = target.mimeType();
		String next = mediaType;
		target.setMediaType(mediaType);
		if (previous != null) {
			try {
				URI uri = vf.createURI("urn:mimetype:" + previous);
				con.removeDesignations(target, uri);
			} catch (IllegalArgumentException e) {
				// invalid mimetype
			}
		}
		if (next != null) {
			URI uri = vf.createURI("urn:mimetype:" + target.mimeType());
			target = (WebResource) con.addDesignations(target, uri);
		}
		return target;
	}

	protected void removeMediaType() throws RepositoryException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		WebResource target = getWebResource();
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

	protected Response invokeMethod(Request req, boolean isResponsePresent)
			throws Throwable {
		boolean isMethodPresent = false;
		String name = req.getOperation();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(isResponsePresent).get(
					name);
			if (methods != null) {
				isMethodPresent = true;
				Method method = findBestMethod(req, methods);
				if (method != null) {
					return invoke(method, req, isResponsePresent);
				}
			}
		}
		List<Method> methods = getPostMethods().get(req.getMethod());
		if (methods != null) {
			isMethodPresent = true;
			Method method = findBestMethod(req, methods);
			if (method != null) {
				return invoke(method, req, isResponsePresent);
			}
		}
		if (isMethodPresent)
			return new Response().badRequest();
		if (req.isQueryStringPresent())
			return methodNotAllowed(req);
		return null;
	}

	protected Response invokeMethod(Request req) throws Throwable {
		List<Method> methods = getPostMethods().get(req.getMethod());
		if (methods != null) {
			Method method = findBestMethod(req, methods);
			if (method == null)
				return new Response().badRequest();
			return invoke(method, req, false);
		}
		return methodNotAllowed(req);
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
				if (names == null) {
					if (!req.isReadable(ptypes[i], gtypes[i]))
						continue loop;
				}
			}
			best = method;
			if (!method.getReturnType().equals(Void.TYPE)) {
				if (method.isAnnotationPresent(type.class)) {
					for (String media : method.getAnnotation(type.class)
							.value()) {
						if (!req.isAcceptable(method.getReturnType(), media))
							continue;
						return best;
					}
					continue loop;
				} else {
					if (!req.isAcceptable(method.getReturnType()))
						continue loop;
				}
			}
			return best;
		}
		return best;
	}

	private Response invoke(Method method, Request req, boolean safe) throws Throwable {
		try {
			Object[] args;
			try {
				args = getParameters(method, req);
			} catch (Exception e) {
				return new Response().badRequest(e);
			}
			try {
				ObjectConnection con = getObjectConnection();
				assert !con.isAutoCommit();
				Object entity = method.invoke(target, args);
				if (safe) {
					con.rollback();
				} else {
					con.setAutoCommit(true); // flush()
					Resource id = target.getResource();
					target = con.getObject(WebResource.class, id);
				}
				return createResponse(method, entity);
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

	private Response createResponse(Method method, Object entity) {
		if (entity instanceof RDFObjectBehaviour) {
			entity = ((RDFObjectBehaviour) entity).getBehaviourDelegate();
		}
		Response rb = new Response();
		if (method.isAnnotationPresent(cacheControl.class)) {
			for (String value : method.getAnnotation(
					cacheControl.class).value()) {
				rb.header("Cache-Control", value);
			}
		}
		if (method.getReturnType().equals(Set.class)) {
			Set set = (Set) entity;
			Iterator iter = set.iterator();
			try {
				if (!iter.hasNext())
					return rb.notFound();
				entity = iter.next();
				if (iter.hasNext()) {
					return rb.entity(set, target);
				}
			} finally {
				getObjectConnection().close(iter);
			}
		}
		if (entity instanceof RDFResource && !getTarget().equals(entity)) {
			RDFResource rdf = (RDFResource) entity;
			Resource resource = rdf.getResource();
			if (resource instanceof URI) {
				URI uri = (URI) resource;
				rb.eTag(target);
				return rb.status(303).location(uri.stringValue());
			}
		}
		return rb.entity(entity, target);
	}

	private Object[] getParameters(Method method, Request req)
			throws RepositoryException, IOException, MimeTypeParseException {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] names = getParameterNames(anns[i]);
			if (names == null) {
				args[i] = req.getBody(ptypes[i], gtypes[i]);
			} else if (names.length == 0
					&& ptypes[i].isAssignableFrom(Map.class)) {
				args[i] = req.getParameterMap();
			} else {
				args[i] = req.getParameter(names, gtypes[i], ptypes[i]);
			}
		}
		return args;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] instanceof parameter)
				return ((parameter) annotations[i]).value();
		}
		return null;
	}

}
