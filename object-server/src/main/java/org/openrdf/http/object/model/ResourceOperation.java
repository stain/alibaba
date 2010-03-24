/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.model;

import info.aduna.net.ParsedURI;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeTypeParseException;
import javax.tools.FileObject;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.HttpEntityEnclosingRequest;
import org.openrdf.http.object.annotations.cacheControl;
import org.openrdf.http.object.annotations.encoding;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.realm;
import org.openrdf.http.object.annotations.rel;
import org.openrdf.http.object.annotations.transform;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.concepts.Transaction;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.http.object.exceptions.NotAcceptable;
import org.openrdf.http.object.exceptions.UnsupportedMediaType;
import org.openrdf.http.object.traits.Realm;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.http.object.util.Accepter;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.iri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class ResourceOperation extends ResourceRequest {
	private static int MAX_TRANSFORM_DEPTH = 100;
	private Logger logger = LoggerFactory.getLogger(ResourceOperation.class);

	private Method method;
	private Method transformMethod;
	private MethodNotAllowed notAllowed;
	private BadRequest badRequest;
	private NotAcceptable notAcceptable;
	private UnsupportedMediaType unsupportedMediaType;
	private List<?> realms;
	private String[] realmURIs;

	public ResourceOperation(File dataDir, HttpEntityEnclosingRequest request,
			ObjectRepository repository) throws QueryEvaluationException,
			RepositoryException, MimeTypeParseException {
		super(dataDir, request, repository);
	}

	public void init() throws MimeTypeParseException, RepositoryException,
			QueryEvaluationException {
		super.init();
		if (method == null) {
			try {
				String m = getMethod();
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
			} catch (NotAcceptable e) {
				notAcceptable = e;
			} catch (UnsupportedMediaType e) {
				unsupportedMediaType = e;
			}
		}
	}

	public String getResponseContentType() throws MimeTypeParseException {
		Method m = getTransformMethod();
		if (m == null || m.getReturnType().equals(Void.TYPE))
			return null;
		if (URL.class.equals(m.getReturnType()))
			return null;
		return getContentType(m);
	}

	public String getResponseContentEncoding() {
		Method m = getTransformMethod();
		if (m == null || m.getReturnType().equals(Void.TYPE))
			return null;
		if (URL.class.equals(m.getReturnType()))
			return null;
		if (!m.isAnnotationPresent(encoding.class))
			return null;
		StringBuilder sb = new StringBuilder();
		for (String value : m.getAnnotation(encoding.class).value()) {
			sb.append(",").append(value);
		}
		return sb.substring(1);
	}

	public String getEntityTag(String contentType)
			throws MimeTypeParseException {
		VersionedObject target = getRequestedResource();
		Method m = this.method;
		int headers = getHeaderCodeFor(m);
		String method = getMethod();
		if (contentType != null) {
			return target.variantTag(contentType, headers);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			if (m != null && contentType == null)
				return target.revisionTag(headers);
			if (m != null)
				return target.variantTag(contentType, headers);
			Method operation;
			if ((operation = getOperationMethod("alternate")) != null) {
				String type = getContentType(getTransformMethodOf(operation));
				headers = getHeaderCodeFor(operation);
				return target.variantTag(type, headers);
			} else if ((operation = getOperationMethod("describedby")) != null) {
				String type = getContentType(getTransformMethodOf(operation));
				headers = getHeaderCodeFor(operation);
				return target.variantTag(type, headers);
			}
		} else if ("PUT".equals(method)) {
			Method get;
			try {
				headers = 0;
				get = findMethod("GET", true);
				headers = getHeaderCodeFor(get);
				get = getTransformMethodOf(get);
			} catch (MethodNotAllowed e) {
				get = null;
			} catch (BadRequest e) {
				get = null;
			} catch (NotAcceptable e) {
				get = null;
			} catch (UnsupportedMediaType e) {
				get = null;
			}
			if (get == null) {
				return target.variantTag(getResponseContentType(), headers);
			} else if (URL.class.equals(get.getReturnType())) {
				return target.revisionTag(headers);
			} else {
				return target.variantTag(getContentType(get), headers);
			}
		} else {
			Method get;
			try {
				headers = 0;
				get = findMethod("GET", true);
				headers = getHeaderCodeFor(get);
				get = getTransformMethodOf(get);
			} catch (MethodNotAllowed e) {
				get = null;
			} catch (BadRequest e) {
				get = null;
			} catch (NotAcceptable e) {
				get = null;
			} catch (UnsupportedMediaType e) {
				get = null;
			}
			if (get == null || URL.class.equals(get.getReturnType())) {
				return target.revisionTag(headers);
			} else {
				return target.variantTag(getContentType(get), headers);
			}
		}
		return null;
	}

	public Class<?> getEntityType() throws MimeTypeParseException {
		String method = getMethod();
		Method m = getTransformMethod();
		if (m == null || "PUT".equals(method) || "DELETE".equals(method)
				|| "OPTIONS".equals(method))
			return null;
		return m.getReturnType();
	}

	public long getLastModified() throws MimeTypeParseException {
		String method = getMethod();
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
		VersionedObject target = getRequestedResource();
		if (mustReevaluate(target.getClass()))
			return System.currentTimeMillis() / 1000 * 1000;
		if (target instanceof FileObject)
			return ((FileObject) target).getLastModified() / 1000 * 1000;
		Transaction trans = target.getRevision();
		if (trans != null) {
			XMLGregorianCalendar xgc = trans.getCommittedOn();
			if (xgc != null) {
				GregorianCalendar cal = xgc.toGregorianCalendar();
				cal.set(Calendar.MILLISECOND, 0);
				return cal.getTimeInMillis();
			}
		}
		return 0;
	}

	public String getResponseCacheControl() {
		if (!isStorable())
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
			setCacheControl(getRequestedResource().getClass(), sb);
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

	public boolean isAuthenticating() {
		return getRealmURIs().length > 0;
	}

	public Set<String> getAllowedMethods() throws RepositoryException {
		Set<String> set = new LinkedHashSet<String>();
		String name = getOperation();
		File file = getFile();
		RDFObject target = getRequestedResource();
		if (!isQueryStringPresent() && file != null && file.canRead()
				|| getOperationMethods("GET", true).containsKey(name)) {
			set.add("GET");
			set.add("HEAD");
		}
		if (!isQueryStringPresent() && file != null) {
			if (!file.exists() || file.canWrite()) {
				set.add("PUT");
			}
			if (file.exists() && file.getParentFile().canWrite()) {
				set.add("DELETE");
			}
		} else if (getOperationMethods("PUT", false).containsKey(name)) {
			set.add("PUT");
		} else if (getOperationMethods("DELETE", false).containsKey(name)) {
			set.add("DELETE");
		}
		Map<String, List<Method>> map = getPostMethods(target);
		for (String method : map.keySet()) {
			set.add(method);
		}
		return set;
	}

	public Method getJavaMethod() {
		if (notAllowed != null)
			throw notAllowed;
		if (badRequest != null)
			throw badRequest;
		if (notAcceptable != null)
			throw notAcceptable;
		if (unsupportedMediaType != null)
			throw unsupportedMediaType;
		return method;
	}

	public Method getOperationMethod(String rel) throws MimeTypeParseException {
		Map<String, List<Method>> map = getOperationMethods("GET", true);
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			for (Method m : e.getValue()) {
				if (m.isAnnotationPresent(rel.class)) {
					for (String value : m.getAnnotation(rel.class).value()) {
						if (rel.equals(value) && isAcceptable(m)) {
							return m;
						}
					}
				}
			}
		}
		return null;
	}

	public Map<String, List<Method>> getOperationMethods(String method,
			Boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : getRequestedResource().getClass().getMethods()) {
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

	public Object[] getParameters(Method method, Entity input) throws Exception {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] types = getParameterMediaTypes(anns[i]);
			args[i] = getParameter(anns[i], ptypes[i], input).read(ptypes[i],
					gtypes[i], types);
		}
		return args;
	}

	public ResponseEntity invoke(Method method, Object[] args, boolean follow)
			throws Exception {
		Object result = method.invoke(getRequestedResource(), args);
		ResponseEntity input = createResultEntity(result, method
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

	private boolean isRequestBody(Method method) {
		for (Annotation[] anns : method.getParameterAnnotations()) {
			if (getParameterNames(anns) == null && getHeaderNames(anns) == null)
				return true;
		}
		return false;
	}

	private String[] getTypes(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	private Method findBestMethod(List<Method> methods)
			throws MimeTypeParseException {
		Method best = null;
		boolean readable = true;
		boolean acceptable = true;
		loop: for (Method method : methods) {
			if (!isReadable(getBody(), method, 0)) {
				readable = false;
				continue loop;
			}
			if (method.getReturnType().equals(Void.TYPE)
					|| method.getReturnType().equals(URL.class)
					|| isAcceptable(method, 0)) {
				panns: for (Annotation[] anns : method
						.getParameterAnnotations()) {
					for (Annotation ann : anns) {
						if (ann.annotationType().equals(parameter.class))
							continue panns;
						if (ann.annotationType().equals(header.class))
							continue panns;
					}
					for (Annotation ann : anns) {
						if (ann.annotationType().equals(type.class)) {
							Accepter accepter = new Accepter(((type) ann)
									.value());
							if (accepter.isAcceptable(getResponseContentType()))
								return method; // compatible
							continue loop; // incompatible
						}
					}
				}
				best = method;
			} else {
				acceptable = false;
			}
		}
		if (best == null && !readable)
			throw new UnsupportedMediaType();
		if (best == null && !acceptable)
			throw new NotAcceptable();
		return best;
	}

	private Method findMethod(String method) throws MimeTypeParseException {
		return findMethod(method, null);
	}

	private Method findMethod(String req_method, Boolean isResponsePresent)
			throws MimeTypeParseException {
		Method method = null;
		boolean isMethodPresent = false;
		String name = getOperation();
		RDFObject target = getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(req_method,
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
				if (name != null && isOperationProhibited(m))
					continue;
				methods.add(m);
			}
			if (!methods.isEmpty()) {
				isMethodPresent = true;
				method = findBestMethod(methods);
			}
		}
		if (method == null) {
			if (isMethodPresent)
				throw new BadRequest();
			throw new MethodNotAllowed();
		}
		return method;
	}

	private boolean isOperationProhibited(Method m) {
		return m.isAnnotationPresent(operation.class)
				&& m.getAnnotation(operation.class).value().length == 0;
	}

	private Entity getParameter(Annotation[] anns, Class<?> ptype, Entity input)
			throws Exception {
		String[] names = getParameterNames(anns);
		String[] headers = getHeaderNames(anns);
		String[] types = getParameterMediaTypes(anns);
		if (names == null && headers == null) {
			return getValue(anns, input);
		} else if (headers != null) {
			return getValue(anns, getHeader(types, headers));
		} else if (names.length == 1 && names[0].equals("*")) {
			return getValue(anns, getQueryString(types));
		} else {
			return getValue(anns, getParameter(types, names));
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

	private Map<String, List<Method>> getPostMethods(RDFObject target) {
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
		for (Method m : getRequestedResource().getClass().getMethods()) {
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

	private int getHeaderCodeFor(Method method) throws MimeTypeParseException {
		if (method == null)
			return 0;
		Set<String> names = getHeaderNamesFor(method, new HashSet<String>());
		if (names.isEmpty())
			return 0;
		Map<String, String> headers = new HashMap<String, String>();
		for (String name : names) {
			Enumeration e = getHeaderEnumeration(name);
			while (e.hasMoreElements()) {
				String value = e.nextElement().toString();
				if (headers.containsKey(name)) {
					headers.put(name, headers.get(name) + "," + value);
				} else {
					headers.put(name, value);
				}
			}
		}
		return headers.hashCode();
	}

	private Set<String> getHeaderNamesFor(Method method, Set<String> names)
			throws MimeTypeParseException {
		for (Annotation[] anns : method.getParameterAnnotations()) {
			String[] ar = getHeaderNames(anns);
			if (ar != null) {
				names.addAll(Arrays.asList(ar));
			}
		}
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				Method transform = getTransform(uri);
				if (isAcceptable(transform, 0))
					return getHeaderNamesFor(transform, names);
			}
		}
		return names;
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
				if (isAcceptable(media, method.getReturnType(), method
						.getGenericReturnType()))
					return true;
			}
			return false;
		} else {
			return isAcceptable(method.getReturnType(), method
					.getGenericReturnType());
		}
	}

	private boolean isReadable(Entity input, Annotation[] anns, Class<?> ptype,
			Type gtype, int depth) throws MimeTypeParseException {
		if (getHeaderNames(anns) != null)
			return true;
		if (getParameterNames(anns) != null)
			return true;
		for (String uri : getTransforms(anns)) {
			if (isReadable(input, getTransform(uri), ++depth))
				return true;
		}
		return input.isReadable(ptype, gtype, getParameterMediaTypes(anns));
	}

	private boolean isReadable(Entity input, Method method, int depth)
			throws MimeTypeParseException {
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

	private String[] getRealmURIs() {
		if (realmURIs != null)
			return realmURIs;
		RDFObject target = getRequestedResource();
		if (method != null && method.isAnnotationPresent(realm.class)) {
			realmURIs = method.getAnnotation(realm.class).value();
		} else {
			ArrayList<String> list = new ArrayList<String>();
			addRealms(list, target.getClass());
			if (Realm.OPERATIONS.contains(getOperation())) {
				list.remove(getURI());
			}
			realmURIs = list.toArray(new String[list.size()]);
		}
		ParsedURI base = null;
		for (int i = 0; i < realmURIs.length; i++) {
			if (realmURIs[i].startsWith("/")) {
				if (base == null) {
					base = new ParsedURI(target.getResource().stringValue());
				}
				realmURIs[i] = base.resolve(realmURIs[i]).toString();
			}
		}
		return realmURIs;
	}

	public List<?> getRealms() throws QueryEvaluationException,
			RepositoryException {
		if (realms != null)
			return realms;
		String[] values = getRealmURIs();
		if (values.length == 0)
			return Collections.emptyList();
		ObjectConnection con = getObjectConnection();
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
