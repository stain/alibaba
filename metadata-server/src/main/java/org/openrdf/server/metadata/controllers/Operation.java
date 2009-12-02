/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeTypeParseException;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.server.metadata.WebObject;
import org.openrdf.server.metadata.annotations.cacheControl;
import org.openrdf.server.metadata.annotations.header;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.annotations.parameter;
import org.openrdf.server.metadata.annotations.realm;
import org.openrdf.server.metadata.annotations.rel;
import org.openrdf.server.metadata.annotations.title;
import org.openrdf.server.metadata.annotations.transform;
import org.openrdf.server.metadata.annotations.type;
import org.openrdf.server.metadata.concepts.Realm;
import org.openrdf.server.metadata.concepts.WebRedirect;
import org.openrdf.server.metadata.exceptions.BadRequest;
import org.openrdf.server.metadata.exceptions.MethodNotAllowed;
import org.openrdf.server.metadata.http.Entity;
import org.openrdf.server.metadata.http.Request;
import org.openrdf.server.metadata.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operation {
	private static int MAX_TRANSFORM_DEPTH = 100;
	private Logger logger = LoggerFactory.getLogger(Operation.class);

	private boolean exists;
	private Request req;
	private Method method;
	private Method transformMethod;
	private MethodNotAllowed notAllowed;
	private BadRequest badRequest;
	private List<Realm> realms;
	private String[] realmURIs;

	public Operation(Request req, boolean exists)
			throws MimeTypeParseException, QueryEvaluationException,
			RepositoryException {
		this.req = req;
		this.exists = exists;
		try {
			String m = req.getMethod();
			if ("GET".equals(m) || "HEAD".equals(m)) {
				method = findMethod(m, true);
			} else if ("PUT".equals(m) || "DELETE".equals(m)) {
				method = findMethod(m, false);
			} else {
				method = findMethod(m);
			}
			transformMethod = getTransformMethodOf(method);
		} catch (MethodNotAllowed e) {
			notAllowed = e;
		} catch (BadRequest e) {
			badRequest = e;
		}
	}

	public String toString() {
		if (method != null)
			return method.getName();
		return req.toString();
	}

	public String getContentType() throws MimeTypeParseException {
		Method m = getTransformMethod();
		if (m == null && exists)
			return req.getRequestedResource().getMediaType();
		if (m == null || m.getReturnType().equals(Void.TYPE))
			return null;
		if (URL.class.equals(m.getReturnType()))
			return null;
		return req.getContentType(m);
	}

	public String getEntityTag(String contentType)
			throws MimeTypeParseException {
		WebObject target = req.getRequestedResource();
		Method m = this.method;
		String method = req.getMethod();
		if (m == null && exists) {
			return req.getRequestedResource().identityTag();
		} else if (contentType != null) {
			return target.variantTag(contentType);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			if (m != null && contentType == null)
				return target.revisionTag();
			if (m != null)
				return target.variantTag(contentType);
			if (!exists && !(target instanceof WebRedirect)) {
				Method operation;
				if ((operation = getOperationMethod("alternate")) != null) {
					return target.variantTag(req.getContentType(operation));
				} else if ((operation = getOperationMethod("describedby")) != null) {
					return target.variantTag(req.getContentType(operation));
				}
			}
		} else if ("PUT".equals(method)) {
			Method get;
			try {
				get = getTransformMethodOf(findMethod("GET", true));
			} catch (MethodNotAllowed e) {
				get = null;
			} catch (BadRequest e) {
				get = null;
			}
			String media = target.getMediaType();
			if (get == null && media == null) {
				return null;
			} else if (get == null && media.equals(req.getContentType())) {
				return target.identityTag();
			} else if (get == null) {
				return target.variantTag(req.getContentType());
			} else if (URL.class.equals(get.getReturnType())) {
				return target.revisionTag();
			} else {
				return target.variantTag(req.getContentType(get));
			}
		} else {
			Method get;
			try {
				get = getTransformMethodOf(findMethod("GET", true));
			} catch (MethodNotAllowed e) {
				get = null;
			} catch (BadRequest e) {
				get = null;
			}
			String media = target.getMediaType();
			if (get == null && media == null) {
				return null;
			} else if (get == null || URL.class.equals(get.getReturnType())) {
				return target.revisionTag();
			} else {
				return target.variantTag(req.getContentType(get));
			}
		}
		return null;
	}

	public Class<?> getEntityType() throws MimeTypeParseException {
		String method = req.getMethod();
		Method m = getTransformMethod();
		if (m == null || "PUT".equals(method) || "DELETE".equals(method)
				|| "OPTIONS".equals(method))
			return null;
		if (m == null && exists)
			return File.class;
		return m.getReturnType();
	}

	public long getLastModified() throws MimeTypeParseException {
		String method = req.getMethod();
		Method m = this.method;
		if (m != null && !"PUT".equals(method) && !"DELETE".equals(method)
				&& !"OPTIONS".equals(method)) {
			if (m.isAnnotationPresent(cacheControl.class)) {
				for (String value : m.getAnnotation(cacheControl.class).value()) {
					if (value.contains("must-reevaluate"))
						return System.currentTimeMillis() / 1000 * 1000;
				}
			}
		}
		WebObject target = req.getRequestedResource();
		if (mustReevaluate(target.getClass()))
			return System.currentTimeMillis() / 1000 * 1000;
		if (m == null && exists)
			return req.getFile().lastModified() / 1000 * 1000;
		return target.getLastModified();
	}

	public List<String> getLinks() throws RepositoryException {
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
					for (String value : getTypes(m)) {
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

	public String getCacheControl() {
		if (!req.isStorable())
			return null;
		StringBuilder sb = new StringBuilder();
		if (method != null && method.isAnnotationPresent(cacheControl.class)) {
			for (String value : method.getAnnotation(cacheControl.class)
					.value()) {
				if (value != null) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(value);
				}
			}
		}
		if (sb.length() <= 0) {
			setCacheControl(req.getRequestedResource().getClass(), sb);
		}
		if (sb.indexOf("private") < 0 && sb.indexOf("public") < 0) {
			if (isAuthenticating() && sb.indexOf("s-maxage") < 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("s-maxage=0");
			} else if (!isAuthenticating()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("public");
			}
		}
		if (sb.length() > 0)
			return sb.toString();
		return null;
	}

	public String allowOrigin() throws QueryEvaluationException,
			RepositoryException {
		List<Realm> realms = getRealms();
		StringBuilder sb = new StringBuilder();
		for (Realm realm : realms) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			String origin = realm.allowOrigin();
			if ("*".equals(origin))
				return origin;
			if (origin != null && origin.length() > 0) {
				sb.append(origin);
			}
		}
		return sb.toString();
	}

	public boolean isAuthenticating() {
		return getRealmURIs().length > 0;
	}

	public boolean isVaryOrigin() throws QueryEvaluationException,
			RepositoryException {
		List<Realm> realms = getRealms();
		for (Realm realm : realms) {
			String allowed = realm.allowOrigin();
			if (allowed != null && allowed.length() > 0)
				return true;
		}
		return false;
	}

	public boolean isAuthorized() throws QueryEvaluationException,
			RepositoryException {
		String ad = req.getRemoteAddr();
		String m = req.getMethod();
		String o = req.getHeader("Origin");
		String au = req.getHeader("Authorization");
		String f = null;
		String al = null;
		byte[] e = null;
		X509Certificate cret = req.getX509Certificate();
		if (cret != null) {
			PublicKey pk = cret.getPublicKey();
			f = pk.getFormat();
			al = pk.getAlgorithm();
			e = pk.getEncoded();
		}
		List<Realm> realms = getRealms();
		for (Realm realm : realms) {
			String allowed = realm.allowOrigin();
			if (allowed != null && allowed.length() > 0) {
				if (o != null && o.length() > 0 && !isOriginAllowed(allowed, o))
					continue;
			}
			if (au == null) {
				if (realm.authorize(ad, m, f, al, e))
					return true;
			} else {
				String url = req.getRequestURL();
				if (realm.authorize(ad, m, url, au, f, al, e))
					return true;
			}
		}
		return false;
	}

	public InputStream unauthorized() throws QueryEvaluationException,
			RepositoryException, IOException {
		List<Realm> realms = getRealms();
		for (Realm realm : realms) {
			InputStream auth = realm.unauthorized();
			if (auth != null)
				return auth;
		}
		return null;
	}

	protected Collection<String> getAllowedHeaders() {
		if (method == null)
			return Collections.emptyList();
		List<String> result = null;
		for (Annotation[] anns : method.getParameterAnnotations()) {
			for (Annotation ann : anns) {
				if (ann.annotationType().equals(header.class)) {
					if (result == null) {
						result = new ArrayList<String>();
					}
					result.addAll(Arrays.asList(((header)ann).value()));
				}
			}
		}
		if (result == null)
			return Collections.emptyList();
		return result;
	}

	protected Set<String> getAllowedMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = req.getOperation();
		File file = req.getFile();
		WebObject target = req.getRequestedResource();
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

	protected Method getMethod() {
		if (notAllowed != null)
			throw notAllowed;
		if (badRequest != null)
			throw badRequest;
		return method;
	}

	protected Method getOperationMethod(String rel)
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

	protected Map<String, List<Method>> getOperationMethods(WebObject target,
			String method, Boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			boolean content = !m.getReturnType().equals(Void.TYPE);
			if (isRespBody != null && isRespBody != content)
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
			} else if ("OPTIONS".equals(method)) {
				put(map, ann.value(), m);
			} else {
				boolean body = isRequestBody(m);
				if (("GET".equals(method) || "HEAD".equals(method)) && content
						&& !body) {
					put(map, ann.value(), m);
				} else if (("PUT".equals(method) || "DELETE".equals(method))
						&& !content && body) {
					put(map, ann.value(), m);
				} else if ("POST".equals(method) && content && body) {
					put(map, ann.value(), m);
				}
			}
		}
		return map;
	}

	private boolean isRequestBody(Method method) {
		for (Annotation[] anns : method.getParameterAnnotations()) {
			if (getParameterNames(anns) == null && getHeaderNames(anns) == null)
				return true;
		}
		return false;
	}

	protected Object[] getParameters(Method method, Entity input)
			throws Exception {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = getParameter(anns[i], ptypes[i], input).read(ptypes[i],
					gtypes[i]);
		}
		return args;
	}

	protected ResponseEntity invoke(Method method, Object[] args, boolean follow)
			throws Exception {
		Object result = method.invoke(req.getRequestedResource(), args);
		ResponseEntity input = req.createResultEntity(result, method
				.getReturnType(), method.getGenericReturnType(),
				getTypes(method));
		if (follow && method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				Method transform = getTransform(uri);
				if (isAcceptable(transform, 0)) {
					return invoke(transform, getParameters(transform, input),
							follow);
				}
			}
		}
		return input;
	}

	private String[] getTypes(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	private Method findBestMethod(List<Method> methods)
			throws MimeTypeParseException {
		Method best = null;
		loop: for (Method method : methods) {
			if (!isReadable(req.getBody(), method, 0))
				continue loop;
			if (best == null) {
				best = method; // readable
			}
			panns: for (Annotation[] anns : method.getParameterAnnotations()) {
				for (Annotation ann : anns) {
					if (ann.annotationType().equals(parameter.class))
						continue panns;
					if (ann.annotationType().equals(header.class))
						continue panns;
				}
				for (Annotation ann : anns) {
					if (ann.annotationType().equals(type.class)) {
						for (String type : ((type)ann).value()) {
							if (req.isCompatible(type)) {
								best = method; // compatible
								break panns;
							}
						}
						continue loop; // incompatible
					}
				}
			}
			if (!method.getReturnType().equals(Void.TYPE)) {
				if (!isAcceptable(method, 0))
					continue loop;
			}
			return method; // acceptable
		}
		return best;
	}

	private Method findMethod(String method) throws MimeTypeParseException {
		return findMethod(method, null);
	}

	private Method findMethod(String req_method, Boolean isResponsePresent)
			throws MimeTypeParseException {
		Method method = null;
		boolean isMethodPresent = false;
		String name = req.getOperation();
		WebObject target = req.getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(target, req_method,
					isResponsePresent).get(name);
			if (methods != null) {
				isMethodPresent = true;
				method = findBestMethod(methods);
			}
		}
		if (method == null) {
			List<Method> methods = new ArrayList<Method>();
			for (Method m : target.getClass().getMethods()) {
				method ann = m.getAnnotation(method.class);
				if (ann == null)
					continue;
				if (!Arrays.asList(ann.value()).contains(req_method))
					continue;
				if (m.isAnnotationPresent(operation.class))
					continue;
				methods.add(m);
			}
			if (!methods.isEmpty()) {
				method = findBestMethod(methods);
				if (method == null)
					throw new BadRequest();
			}
		}
		if (method == null) {
			if (isMethodPresent)
				throw new BadRequest();
			if (req.isQueryStringPresent())
				throw new MethodNotAllowed();
		}
		return method;
	}

	private Entity getParameter(Annotation[] anns, Class<?> ptype, Entity input)
			throws Exception {
		String[] names = getParameterNames(anns);
		String[] headers = getHeaderNames(anns);
		String[] types = getParameterMediaTypes(anns);
		if (names == null && headers == null) {
			return getValue(anns, input);
		} else if (headers != null) {
			return getValue(anns, req.getHeader(types, headers));
		} else if (names.length == 1 && names[0].equals("*")) {
			return getValue(anns, req.getQueryString(types));
		} else {
			return getValue(anns, req.getParameter(types, names));
		}
	}

	private Entity getValue(Annotation[] anns, Entity input) throws Exception {
		for (String uri : getTransforms(anns)) {
			Method transform = getTransform(uri);
			if (isReadable(input, transform, 0)) {
				Object[] args = getParameters(transform, input);
				return invoke(transform, args, false);
			}
		}
		return input;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(parameter.class))
				return ((parameter) annotations[i]).value();
		}
		return null;
	}

	private String[] getHeaderNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(header.class))
				return ((header) annotations[i]).value();
		}
		return null;
	}

	private String[] getParameterMediaTypes(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(type.class))
				return ((type) annotations[i]).value();
		}
		return null;
	}

	private Map<String, List<Method>> getPostMethods(WebObject target) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			method ann = m.getAnnotation(method.class);
			if (ann == null) {
				if (m.isAnnotationPresent(operation.class)
						&& !m.getReturnType().equals(Void.TYPE)
						&& isRequestBody(m)) {
					put(map, new String[] { "POST" }, m);
				}
			} else {
				put(map, ann.value(), m);
			}
		}
		return map;
	}

	private Method getTransform(String uri) {
		for (Method m : req.getRequestedResource().getClass().getMethods()) {
			if (m.isAnnotationPresent(iri.class)) {
				if (uri.equals(m.getAnnotation(iri.class).value())) {
					return m;
				}
			}
		}
		logger.warn("Method not found: {}", uri);
		return null;
	}

	private Method getTransformMethod() {
		return transformMethod;
	}

	private Method getTransformMethodOf(Method method)
			throws MimeTypeParseException {
		if (method == null)
			return method;
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				Method transform = getTransform(uri);
				if (isAcceptable(transform, 0))
					return getTransformMethodOf(transform);
			}
		}
		return method;
	}

	private String[] getTransforms(Annotation[] anns) {
		for (Annotation ann : anns) {
			if (ann.annotationType().equals(transform.class)) {
				return ((transform) ann).value();
			}
		}
		return new String[0];
	}

	private boolean isAcceptable(Method method, int depth)
			throws MimeTypeParseException {
		if (method == null)
			return false;
		if (depth > MAX_TRANSFORM_DEPTH) {
			logger.error("Max transform depth exceeded: {}", method.getName());
			return false;
		}
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				if (isAcceptable(getTransform(uri), ++depth))
					return true;
			}
		}
		if (method.isAnnotationPresent(type.class)) {
			for (String media : getTypes(method)) {
				if (req.isAcceptable(media, method.getReturnType(), method
						.getGenericReturnType()))
					return true;
			}
			return false;
		} else {
			return req.isAcceptable(method.getReturnType(), method
					.getGenericReturnType());
		}
	}

	private boolean isReadable(Entity input, Annotation[] anns, Class<?> ptype,
			Type gtype, int depth) {
		if (getHeaderNames(anns) != null)
			return true;
		if (getParameterNames(anns) != null)
			return true;
		for (String uri : getTransforms(anns)) {
			if (isReadable(input, getTransform(uri), ++depth))
				return true;
		}
		return input.isReadable(ptype, gtype);
	}

	private boolean isReadable(Entity input, Method method, int depth) {
		if (method == null)
			return false;
		if (depth > MAX_TRANSFORM_DEPTH) {
			logger.error("Max transform depth exceeded: {}", method.getName());
			return false;
		}
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			if (!isReadable(input, anns[i], ptypes[i], gtypes[i], depth))
				return false;
		}
		return true;
	}

	private void setCacheControl(Class<?> type, StringBuilder sb) {
		if (type.isAnnotationPresent(cacheControl.class)) {
			for (String value : type.getAnnotation(cacheControl.class).value()) {
				if (value != null) {
					if (sb.length() > 0) {
						sb.append(", ");
					} else {
						sb.append(value);
					}
				}
			}
		} else {
			if (type.getSuperclass() != null) {
				setCacheControl(type.getSuperclass(), sb);
			}
			for (Class<?> face : type.getInterfaces()) {
				setCacheControl(face, sb);
			}
		}
	}

	private boolean isOriginAllowed(String allowed, String o) {
		for (String ao : allowed.split("\\s*,\\s*")) {
			if (o.startsWith(ao))
				return true;
		}
		return false;
	}

	private String[] getRealmURIs() {
		if (realmURIs != null)
			return realmURIs;
		if (method != null && method.isAnnotationPresent(realm.class)) {
			realmURIs = method.getAnnotation(realm.class).value();
		} else {
			ArrayList<String> list = new ArrayList<String>();
			addRealms(list, req.getRequestedResource().getClass());
			realmURIs = list.toArray(new String[list.size()]);
		}
		java.net.URI base = null;
		for (int i=0;i<realmURIs.length;i++) {
			if (realmURIs[i].startsWith("/")) {
				if (base == null) {
					base = req.getRequestedResource().toUri();
				}
				realmURIs[i] = base.resolve(realmURIs[i]).toASCIIString();
			}
		}
		return realmURIs;
	}

	private List<Realm> getRealms() throws QueryEvaluationException,
			RepositoryException {
		if (realms != null)
			return realms;
		String[] values = getRealmURIs();
		if (values.length == 0)
			return Collections.emptyList();
		ObjectConnection con = req.getObjectConnection();
		return realms = con.getObjects(Realm.class, values).asList();
	}

	private void addRealms(ArrayList<String> list, Class<?> type) {
		if (type.isAnnotationPresent(realm.class)) {
			for (String value : type.getAnnotation(realm.class).value()) {
				list.add(value);
			}
		} else {
			if (type.getSuperclass() != null) {
				addRealms(list, type.getSuperclass());
			}
			for (Class<?> face : type.getInterfaces()) {
				addRealms(list, face);
			}
		}
	}

	private boolean mustReevaluate(Class<?> type) {
		if (type.isAnnotationPresent(cacheControl.class)) {
			for (String value : type.getAnnotation(cacheControl.class).value()) {
				if (value.contains("must-reevaluate"))
					return true;
			}
		} else {
			if (type.getSuperclass() != null) {
				if (mustReevaluate(type.getSuperclass()))
					return true;
			}
			for (Class<?> face : type.getInterfaces()) {
				if (mustReevaluate(face))
					return true;
			}
		}
		return false;
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

}
