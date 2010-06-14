/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
package org.openrdf.http.object.handlers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.parameterTypes;

/**
 * Provides debugging information on {@link MethodNotAllowed} errors.
 * 
 * @author James Leigh
 * 
 */
public class DetailedMethodNotAllowedHandler implements Handler {
	private final Handler delegate;

	public DetailedMethodNotAllowedHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public Response verify(ResourceOperation request) throws Exception {
		try {
			return delegate.verify(request);
		} catch (MethodNotAllowed e) {
			throw new MethodNotAllowed(message(request), e);
		}
	}

	public Response handle(ResourceOperation request) throws Exception {
		try {
			return delegate.handle(request);
		} catch (MethodNotAllowed e) {
			throw new MethodNotAllowed(message(request), e);
		}
	}

	private String message(ResourceOperation request) throws RepositoryException {
		RDFObject target = request.getRequestedResource();
		StringBuilder sb = new StringBuilder();
		sb.append("Method Not Allowed\r\n");
		sb.append("\r\n");
		sb.append("This resource implements the following interfaces:\r\n");
		Map<String, Collection<String>> map = getConcepts(target);
		Collection<String> unregistered = map.remove("");
		for (Map.Entry<String, Collection<String>> line : map.entrySet()) {
			sb.append("\t").append(line.getKey());
			for (String type : line.getValue()) {
				sb.append("\t\t<").append(type).append(">");
			}
			sb.append("\r\n");
		}
		sb.append("\r\n");
		if (unregistered != null && !unregistered.isEmpty()) {
			sb.append("This resource has the following unregistered types:\r\n");
			for (String line : unregistered) {
				sb.append("\t").append(line).append("\r\n");
			}
			sb.append("\r\n");
		}
		Collection<String> list = new TreeSet<String>();
		String uri = target.getResource().stringValue();
		for (Method m : target.getClass().getMethods()) {
			method ma = m.getAnnotation(method.class);
			operation oa = m.getAnnotation(operation.class);
			if (ma == null && oa == null)
				continue;
			if (m.isAnnotationPresent(parameterTypes.class))
				continue;
			if (ma == null) {
				if (oa != null) {
					boolean content = !m.getReturnType().equals(Void.TYPE);
					boolean body = request.isRequestBody(m);
					for (String ro : oa.value()) {
						String qs = ro.length() == 0 ? "" : "?" + ro;
						if (content && !body) {
							list.add("GET " + uri + qs);
						} else if (!content && body) {
							list.add("PUT " + uri + qs);
							list.add("DELETE " + uri + qs);
						} else if (content && body) {
							list.add("POST " + uri + qs);
						}
					}
				}
			} else {
				for (String rm : ma.value()) {
					if (oa == null || oa.value().length == 0) {
						list.add(rm + " " + uri);
					} else {
						for (String ro : oa.value()) {
							String qs = ro.length() == 0 ? "" : "?" + ro;
							list.add(rm + " " + uri + qs);
						}
					}
				}
			}
		}
		if (map.isEmpty()) {
			sb.append("This resource does not accept any requests.\r\n");
		} else {
			sb.append("This resource can accept the following requests:\r\n");
			for (String line : list) {
				sb.append("\t").append(line).append("\r\n");
			}
		}
		return sb.toString();
	}

	private Map<String, Collection<String>> getConcepts(RDFObject target) throws RepositoryException {
		Set<String> types = new TreeSet<String>();
		Map<String, Collection<String>> concepts = new TreeMap<String, Collection<String>>();
		ObjectConnection con = target.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		ObjectFactory of = con.getObjectFactory();
		Class<?> global = of.createObject().getClass();
		Collection<String> globals = getTraits(global, new TreeSet<String>());
		RepositoryResult<Statement> stmts = con.getStatements(target.getResource(), RDF.TYPE, null);
		try {
			while (stmts.hasNext()) {
				Value type = stmts.next().getObject();
				if (type instanceof URI) {
					types.add(type.stringValue());
					Class<?> proxy = of.createObject(vf.createBNode(), (URI) type).getClass();
					if (proxy.equals(global))
						continue;
					if (of.isNamedConcept(proxy.getSuperclass())) {
						String key = proxy.getSuperclass().getName();
						Collection<String> set = concepts.get(key);
						if (set == null) {
							concepts.put(key, set = new TreeSet<String>());
						}
						set.add(type.stringValue());
					}
					for (Class<?> f : target.getClass().getInterfaces()) {
						if (of.isNamedConcept(f)) {
							String key = f.getName();
							Collection<String> set = concepts.get(key);
							if (set == null) {
								concepts.put(key, set = new TreeSet<String>());
							}
							set.add(type.stringValue());
						}
					}
				}
			}
		} finally {
			stmts.close();
		}
		concepts.keySet().removeAll(globals);
		Map<String, Collection<String>> map = new TreeMap<String, Collection<String>>();
		Class<?> c = target.getClass();
		if (!Object.class.equals(c.getSuperclass())) {
			String key = c.getSuperclass().getName();
			Collection<String> collection = concepts.get(key);
			if (collection == null) {
				map.put(key, Collections.EMPTY_SET);
			} else {
				map.put(key, collection);
				types.removeAll(collection);
			}
		}
		for (Class<?> f : c.getInterfaces()) {
			Collection<String> collection = concepts.get(f.getName());
			if (collection == null) {
				map.put(f.getName(), Collections.EMPTY_SET);
			} else {
				map.put(f.getName(), collection);
				types.removeAll(collection);
			}
		}
		map.put("", types);
		return map;
	}

	private Collection<String> getTraits(Class<?> global, Collection<String> globals) {
		if (!Object.class.equals(global.getSuperclass()) && global.getSuperclass() != null) {
			globals.add(global.getSuperclass().getName());
			getTraits(global.getSuperclass(), globals);
		}
		for (Class<?> f : global.getInterfaces()) {
			globals.add(f.getName());
			getTraits(f, globals);
		}
		return globals;
	}
}
