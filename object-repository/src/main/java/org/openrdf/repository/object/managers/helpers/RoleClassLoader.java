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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openrdf.repository.object.annotations.factory;
import org.openrdf.repository.object.exceptions.ElmoInitializationException;
import org.openrdf.repository.object.managers.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleClassLoader {
	private final Logger logger = LoggerFactory.getLogger(DirectMapper.class);

	private RoleMapper roleMapper;

	private ClassLoader cl;

	public void setRoleMapper(RoleMapper roleMapper) {
		this.roleMapper = roleMapper;
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	/**
	 * Loads and registers roles listed in resource.
	 */
	public void loadClasses(String roles, boolean concept) {
		try {
			ClassLoader elmo = RoleClassLoader.class.getClassLoader();
			load(cl, roles, concept, load(elmo, roles, concept, new HashSet<URL>()));
		} catch (Exception e) {
			throw new ElmoInitializationException(e);
		}
	}

	public void scan(URL url, String... roles) {
		try {
			Scanner scanner = new Scanner(cl);
			load(scanner.scan(url, roles), cl, false);
		} catch (Exception e) {
			throw new ElmoInitializationException(e);
		}
	}

	private Set<URL> load(ClassLoader cl, String roles, boolean concept, Set<URL> exclude)
			throws IOException, ClassNotFoundException {
		if (cl == null)
			return exclude;
		Scanner scanner = new Scanner(cl, roles);
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
					IOException ioe = new IOException(msg);
					ioe.initCause(e);
					throw ioe;
				}
			}
		}
		return exclude;
	}

	private void load(List<String> roles, ClassLoader cl, boolean concept)
			throws ClassNotFoundException, IOException {
		for (String role : roles) {
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				recordRole(clazz, null, concept);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private void load(Properties p, ClassLoader cl, boolean concept)
			throws ClassNotFoundException, IOException {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String role = (String) e.getKey();
			String types = (String) e.getValue();
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				for (String rdf : types.split("\\s+")) {
					recordRole(clazz, rdf, concept);
				}
			} catch (Exception exc) {
				logger.error(exc.toString(), exc);
			}
		}
	}

	private void recordRole(Class<?> clazz, String uri, boolean concept) {
		if (uri == null || uri.length() == 0) {
			if (clazz.isInterface() || concept) {
				roleMapper.addConcept(clazz);
			} else if (isFactory(clazz)) {
				roleMapper.addFactory(clazz);
			} else {
				roleMapper.addBehaviour(clazz);
			}
		} else {
			if (clazz.isInterface() || concept) {
				roleMapper.addConcept(clazz, uri);
			} else if (isFactory(clazz)) {
				roleMapper.addFactory(clazz);
			} else {
				roleMapper.addBehaviour(clazz, uri);
			}
		}
	}

	private boolean isFactory(Class<?> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers()))
			return false;
		for (Method m : clazz.getMethods()) {
			if (m.isAnnotationPresent(factory.class))
				return true;
		}
		return false;
	}
}
