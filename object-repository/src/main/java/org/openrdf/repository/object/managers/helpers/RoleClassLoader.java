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
package org.openrdf.repository.object.managers.helpers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.annotations.matches;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the annotations, concepts and behaviours into memory.
 * 
 * @author James Leigh
 * 
 */
public class RoleClassLoader {
	private static final String CONCEPTS = "META-INF/org.openrdf.concepts";
	private static final String BEHAVIOURS = "META-INF/org.openrdf.behaviours";
	private static final String ANNOTATIONS = "META-INF/org.openrdf.annotations";

	private final Logger logger = LoggerFactory.getLogger(DirectMapper.class);

	private RoleMapper roleMapper;

	public RoleClassLoader(RoleMapper roleMapper) {
		this.roleMapper = roleMapper;
	}

	/**
	 * Loads and registers roles listed in resource.
	 * 
	 * @throws ObjectStoreConfigException
	 */
	public void loadRoles(ClassLoader cl) throws ObjectStoreConfigException {
		try {
			ClassLoader first = RoleClassLoader.class.getClassLoader();
			Set<URL> loaded;
			loaded = load(new CheckForAnnotation(first), first, ANNOTATIONS, true, new HashSet<URL>());
			loaded = load(new CheckForAnnotation(cl), cl, ANNOTATIONS, true, loaded);
			loaded = load(new CheckForConcept(first), first, CONCEPTS, true, new HashSet<URL>());
			loaded = load(new CheckForConcept(cl), cl, CONCEPTS, true, loaded);
			loaded = load(new CheckForBehaviour(first), first, BEHAVIOURS, false, new HashSet<URL>());
			loaded = load(new CheckForBehaviour(cl), cl, BEHAVIOURS, false, loaded);
		} catch (ObjectStoreConfigException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	public void scan(URL jar, ClassLoader cl) throws ObjectStoreConfigException {
		scan(jar, new CheckForAnnotation(cl), ANNOTATIONS, cl);
		scan(jar, new CheckForConcept(cl), CONCEPTS, cl);
		scan(jar, new CheckForBehaviour(cl), BEHAVIOURS, cl);
	}

	private void scan(URL url, CheckForConcept checker, String role, ClassLoader cl)
			throws ObjectStoreConfigException {
		try {
			Scanner scanner = new Scanner(checker);
			load(scanner.scan(url, role), cl, false);
		} catch (Exception e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	private Set<URL> load(CheckForConcept checker, ClassLoader cl, String roles, boolean concept, Set<URL> exclude)
			throws IOException, ClassNotFoundException, ObjectStoreConfigException {
		if (cl == null)
			return exclude;
		Scanner scanner = new Scanner(checker, roles);
		Enumeration<URL> resources = cl.getResources(roles);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			if (!exclude.contains(url)) {
				exclude.add(url);
				logger.debug("Reading roles from {}", url);
				try {
					Properties p = new Properties();
					p.load(url.openStream());
					if (p.isEmpty()) {
						load(scanner.scan(url), cl, concept);
					} else {
						load(p, cl, concept);
					}
				} catch (IOException e) {
					String msg = e.getMessage() + " in: " + url;
					throw new ObjectStoreConfigException(msg, e);
				} catch (IllegalArgumentException e) {
					String msg = e.getMessage() + " in: " + url;
					throw new ObjectStoreConfigException(msg, e);
				}
			}
		}
		return exclude;
	}

	private void load(List<String> roles, ClassLoader cl, boolean concept)
			throws IOException, ObjectStoreConfigException {
		for (String role : roles) {
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				recordRole(clazz, null, concept);
			} catch (ClassNotFoundException exc) {
				logger.error(exc.toString());
			}
		}
	}

	private void load(Properties p, ClassLoader cl, boolean concept)
			throws ClassNotFoundException, IOException, ObjectStoreConfigException {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String role = (String) e.getKey();
			String types = (String) e.getValue();
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				for (String rdf : types.split("\\s+")) {
					recordRole(clazz, rdf, concept);
				}
			} catch (ClassNotFoundException exc) {
				logger.error(exc.toString());
			}
		}
	}

	private void recordRole(Class<?> clazz, String uri, boolean concept)
			throws ObjectStoreConfigException {
		if (uri == null || uri.length() == 0) {
			if (clazz.isAnnotation()) {
				roleMapper.addAnnotation(clazz);
			} else if (isAnnotationPresent(clazz) || concept) {
				roleMapper.addConcept(clazz);
			} else {
				roleMapper.addBehaviour(clazz);
			}
		} else {
			if (clazz.isAnnotation()) {
				roleMapper.addAnnotation(clazz, new URIImpl(uri));
			} else if (isAnnotationPresent(clazz) || concept) {
				roleMapper.addConcept(clazz, new URIImpl(uri));
			} else {
				roleMapper.addBehaviour(clazz, new URIImpl(uri));
			}
		}
	}

	private boolean isAnnotationPresent(Class<?> clazz) {
		for (Annotation ann : clazz.getAnnotations()) {
			String name = ann.annotationType().getName();
			if (iri.class.getName().equals(name))
				return true;
			if (matches.class.getName().equals(name))
				return true;
		}
		return false;
	}
}
