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
package org.openrdf.repository.object.managers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.HierarchicalRoleMapper;
import org.openrdf.repository.object.managers.helpers.RoleMatcher;
import org.openrdf.repository.object.vocabulary.OBJ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the annotation, concept, and behaviour classes and what rdf:type they
 * should be used with.
 * 
 * @author James Leigh
 * 
 */
public class RoleMapper implements Cloneable {
	private ValueFactory vf;
	private Logger logger = LoggerFactory.getLogger(RoleMapper.class);
	private HierarchicalRoleMapper roleMapper = new HierarchicalRoleMapper();
	private Map<URI, List<Class<?>>> instances = new ConcurrentHashMap<URI, List<Class<?>>>(
			256);
	private RoleMatcher matches = new RoleMatcher();
	private Map<Class<?>, URI> annotations = new HashMap<Class<?>, URI>();
	private Map<Class<?>, Class<?>> complements;
	private Map<Class<?>, List<Class<?>>> intersections;
	private Set<Class<?>> conceptClasses = new HashSet<Class<?>>();
	private Set<Method> triggers = new HashSet<Method>();

	public RoleMapper() {
		this(ValueFactoryImpl.getInstance());
	}

	public RoleMapper(ValueFactory vf) {
		this.vf = vf;
		roleMapper.setURIFactory(vf);
		complements = new ConcurrentHashMap<Class<?>, Class<?>>();
		intersections = new ConcurrentHashMap<Class<?>, List<Class<?>>>();
	}

	public RoleMapper clone() {
		try {
			RoleMapper cloned = (RoleMapper) super.clone();
			cloned.roleMapper = roleMapper.clone();
			cloned.instances = clone(instances);
			cloned.matches = matches.clone();
			cloned.annotations = new HashMap<Class<?>, URI>(annotations);
			cloned.complements = new ConcurrentHashMap<Class<?>, Class<?>>(complements);
			cloned.intersections = clone(intersections);
			cloned.conceptClasses = new HashSet<Class<?>>(conceptClasses);
			cloned.triggers = new HashSet<Method>(triggers);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	private <K, V> Map<K, List<V>> clone(Map<K, List<V>> map) {
		Map<K, List<V>> cloned = new ConcurrentHashMap<K, List<V>>(map);
		for (Map.Entry<K, List<V>> e : cloned.entrySet()) {
			e.setValue(new CopyOnWriteArrayList<V>(e.getValue()));
		}
		return cloned;
	}

	public Collection<Class<?>> getConceptClasses() {
		return conceptClasses;
	}

	public Collection<Method> getTriggerMethods() {
		return triggers;
	}

	public Collection<Class<?>> findIndividualRoles(URI instance,
			Collection<Class<?>> classes) {
		List<Class<?>> list = instances.get(instance);
		if (list != null) {
			classes.addAll(list);
		}
		matches.findRoles(instance.stringValue(), classes);
		return classes;
	}

	public Collection<Class<?>> findAllRoles() {
		Collection<Class<?>> list = roleMapper.findAllRoles();
		list.addAll(annotations.keySet());
		list.addAll(complements.keySet());
		list.addAll(intersections.keySet());
		return list;
	}

	public boolean isRecordedConcept(URI type, ClassLoader cl) {
		if (roleMapper.isTypeRecorded(type)) {
			for (Class<?> role : findAllRoles(type)) {
				if (findType(role) != null)
					return true;
			}
		}
		if ("java:".equals(type.getNamespace())) {
			try {
				synchronized (cl) {
					java.lang.Class.forName(type.getLocalName(), true, cl);
				}
				return true;
			} catch (ClassNotFoundException e) {
				return false;
			}
		}
		return false;
	}

	public Class<?> findInterfaceConcept(URI uri) {
		Class<?> concept = null;
		Class<?> mapped = null;
		Collection<Class<?>> rs = findAllRoles(uri);
		for (Class r : rs) {
			URI type = findType(r);
			if (r.isInterface() && type != null) {
				concept = r;
				if (uri.equals(type)) {
					mapped = r;
					if (r.getSimpleName().equals(uri.getLocalName())) {
						return r;
					}
				}
			}
		}
		if (mapped != null)
			return mapped;
		if (concept != null)
			return concept;
		return null;
	}

	public Class<?> findConcept(URI uri, ClassLoader cl) {
		if (roleMapper.isTypeRecorded(uri)) {
			Class<?> concept = null;
			Class<?> mapped = null;
			Class<?> face = null;
			Collection<Class<?>> rs = findAllRoles(uri);
			for (Class r : rs) {
				URI type = findType(r);
				if (type != null && r.isInterface()) {
					concept = r;
				}
				if (uri.equals(type)) {
					mapped = r;
					if (r.getSimpleName().equals(uri.getLocalName())) {
						return r;
					} else if (r.isInterface()) {
						face = r;
					}
				}
			}
			if (face != null)
				return face;
			if (mapped != null)
				return mapped;
			if (concept != null)
				return concept;
		}
		if ("java:".equals(uri.getNamespace())) {
			try {
				return java.lang.Class.forName(uri.getLocalName(), true, cl);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}
		return null;
	}

	public Collection<Class<?>> findRoles(URI type) {
		return findAdditionalRoles(roleMapper.findRoles(type));
	}

	public Collection<Class<?>> findRoles(Collection<URI> types,
			Collection<Class<?>> roles) {
		return findAdditionalRoles(roleMapper.findRoles(types, roles));
	}

	public Collection<Class<?>> findAdditionalRoles(Collection<Class<?>> classes) {
		if (complements.isEmpty())
			return classes;
		Collection<Class<?>> result = new ArrayList<Class<?>>(
				classes.size() * 2);
		result.addAll(classes);
		addIntersectionsAndComplements(result);
		return result;
	}

	public Collection<URI> findSubTypes(Class<?> role, Collection<URI> rdfTypes) {
		return roleMapper.findSubTypes(role, rdfTypes);
	}

	public URI findType(Class<?> concept) {
		return roleMapper.findType(concept);
	}

	public boolean isNamedTypePresent() {
		return roleMapper.isNamedTypePresent();
	}

	public boolean isIndividualRolesPresent(URI instance) {
		return !matches.isEmpty() || !instances.isEmpty() && instances.containsKey(instance);
	}

	public URI findAnnotation(Class<?> type) {
		return annotations.get(type);
	}

	public Class<?> findAnnotationType(URI uri) {
		for (Map.Entry<Class<?>, URI> e : annotations.entrySet()) {
			if (e.getValue().equals(uri)) {
				return e.getKey();
			}
		}
		return null;
	}

	public boolean isRecordedAnnotation(URI uri) {
		return findAnnotationType(uri) != null;
	}

	public void addAnnotation(Class<?> annotation) {
		if (!annotation.isAnnotationPresent(iri.class))
			throw new IllegalArgumentException("@rdf annotation required in "
					+ annotation.getSimpleName());
		String uri = annotation.getAnnotation(iri.class).value();
		addAnnotation(annotation, new URIImpl(uri));
	}

	public void addAnnotation(Class<?> annotation, URI uri) {
		annotations.put(annotation, uri);
	}

	public void addConcept(Class<?> role) throws ObjectStoreConfigException {
		recordRole(role, role, null, true, true);
	}

	public void addConcept(Class<?> role, URI type)
			throws ObjectStoreConfigException {
		recordRole(role, role, type, true, false);
	}

	public void addBehaviour(Class<?> role) throws ObjectStoreConfigException {
		assertBehaviour(role);
		boolean hasType = false;
		for (Class<?> face : role.getInterfaces()) {
			boolean recorded = recordRole(role, face, null, false, false);
			if (recorded && hasType) {
				throw new ObjectStoreConfigException(role.getSimpleName()
						+ " can only implement one concept");
			} else {
				hasType |= recorded;
			}
		}
		if (!hasType)
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " must implement a concept or mapped explicitly");
	}

	public void addBehaviour(Class<?> role, URI type)
			throws ObjectStoreConfigException {
		assertBehaviour(role);
		recordRole(role, null, type, false, false);
	}

	private void assertBehaviour(Class<?> role)
			throws ObjectStoreConfigException {
		if (isAnnotationPresent(role))
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " cannot have a concept annotation");
		if (role.isInterface())
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " is an interface and not a behaviour");
		for (Method method : role.getDeclaredMethods()) {
			if (isAnnotationPresent(method)
					&& method.getName().startsWith("get"))
				throw new ObjectStoreConfigException(role.getSimpleName()
						+ " cannot have a property annotation");
		}
	}

	private Collection<Class<?>> findAllRoles(URI type) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		for (Class<?> role : findRoles(type)) {
			if (set.add(role)) {
				addInterfaces(set, role.getSuperclass());
				addInterfaces(set, role.getInterfaces());
			}
		}
		return set;
	}

	private void addInterfaces(Set<Class<?>> set, Class<?>... list) {
		for (Class<?> c : list) {
			if (c != null && set.add(c)) {
				addInterfaces(set, c.getSuperclass());
				addInterfaces(set, c.getInterfaces());
			}
		}
	}

	private boolean isAnnotationPresent(AnnotatedElement role)
			throws ObjectStoreConfigException {
		return role.isAnnotationPresent(iri.class);
	}

	private boolean recordRole(Class<?> role, Class<?> elm, URI rdfType,
			boolean concept, boolean base) throws ObjectStoreConfigException {
		URI defType = elm == null ? null : findDefaultType(role, elm);
		boolean hasType = false;
		if (rdfType != null) {
			if (concept) {
				roleMapper.recordConcept(role, rdfType);
			} else {
				roleMapper.recordBehaviour(role, rdfType);
			}
			hasType = true;
		} else if (defType != null) {
			if (concept) {
				roleMapper.recordConcept(role, defType);
			} else {
				roleMapper.recordBehaviour(role, defType);
			}
			hasType = true;
		} else if (elm != null) {
			hasType = recordAnonymous(role, elm, concept);
		}
		if (!hasType && elm != null) {
			for (Class<?> face : elm.getInterfaces()) {
				hasType |= recordRole(role, face, null, concept, false);
			}
		}
		if (!hasType && base) {
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " does not have an RDF type mapping");
		}
		if (concept && !role.isInterface()) {
			conceptClasses.add(role);
		}
		for (Method m : role.getMethods()) {
			if (m.isAnnotationPresent(triggeredBy.class)) {
				triggers.add(m);
			}
		}
		return hasType;
	}

	private boolean recordAnonymous(Class<?> role, Class<?> elm,
			boolean isConcept) throws ObjectStoreConfigException {
		boolean recorded = false;
		for (Annotation ann : elm.getAnnotations()) {
			try {
				URI name = findAnnotation(ann.annotationType());
				if (name == null
						&& ann.annotationType().isAnnotationPresent(iri.class)) {
					addAnnotation(ann.annotationType());
					name = findAnnotation(ann.annotationType());
				}
				if (name == null)
					continue;
				Object value = ann.getClass().getMethod("value").invoke(ann);
				if (OBJ.MATCHES.equals(name)) {
					String[] values = (String[]) value;
					for (String pattern : values) {
						matches.addRoles(pattern, role);
						recorded = true;
					}
				}
				if (OWL.ONEOF.equals(name)) {
					String[] values = (String[]) value;
					for (String instance : values) {
						URI uri = vf.createURI(instance);
						List<Class<?>> list = instances.get(uri);
						if (list == null) {
							list = new CopyOnWriteArrayList<Class<?>>();
							instances.put(uri, list);
						}
						list.add(role);
						recorded = true;
					}
				}
				if (OWL.COMPLEMENTOF.equals(name)) {
					if (value instanceof Class) {
						Class<?> concept = (Class<?>) value;
						recordRole(concept, concept, null, true, true);
						complements.put(role, concept);
						recorded = true;
					} else {
						logger.error("{} must have a value of type java.lang.Class", ann.annotationType());
					}
				}
				if (OWL.INTERSECTIONOF.equals(name)) {
					List<Class<?>> ofs = new ArrayList<Class<?>>();
					for (Object v : (Object[]) value) {
						if (v instanceof Class) {
							Class<?> concept = (Class<?>) v;
							recordRole(concept, concept, null, true, true);
							ofs.add(concept);
						} else {
							logger.error("{} must have a value of type java.lang.Class", ann.annotationType());
						}
					}
					intersections.put(role, ofs);
					recorded = true;
				}
				if (OWL.UNIONOF.equals(name)) {
					for (Object v : (Object[]) value) {
						if (v instanceof Class) {
							Class<?> concept = (Class<?>) v;
							recordRole(concept, concept, null, true, true);
							if (role.isAssignableFrom(concept)) {
								recorded = true; // implied
							} else {
								recorded |= recordRole(role, concept, null,
										isConcept, true);
							}
						} else {
							logger.error("{} must have a value of type java.lang.Class", ann.annotationType());
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				continue;
			}
		}
		return recorded;
	}

	private URI findDefaultType(Class<?> role, AnnotatedElement elm) {
		if (elm.isAnnotationPresent(iri.class)) {
			String value = elm.getAnnotation(iri.class).value();
			if (value != null) {
				return vf.createURI(value);
			}
		}
		return null;
	}

	private void addIntersectionsAndComplements(Collection<Class<?>> roles) {
		for (Map.Entry<Class<?>, List<Class<?>>> e : intersections.entrySet()) {
			Class<?> inter = e.getKey();
			List<Class<?>> of = e.getValue();
			if (!roles.contains(inter) && intersects(roles, of)) {
				roles.add(inter);
			}
		}
		boolean complementAdded = false;
		for (Map.Entry<Class<?>, Class<?>> e : complements.entrySet()) {
			Class<?> comp = e.getKey();
			Class<?> of = e.getValue();
			if (!roles.contains(comp) && !contains(roles, of)) {
				complementAdded = true;
				roles.add(comp);
			}
		}
		if (complementAdded) {
			for (Map.Entry<Class<?>, List<Class<?>>> e : intersections
					.entrySet()) {
				Class<?> inter = e.getKey();
				List<Class<?>> of = e.getValue();
				if (!roles.contains(inter) && intersects(roles, of)) {
					roles.add(inter);
				}
			}
		}
	}

	private boolean intersects(Collection<Class<?>> roles, List<Class<?>> ofs) {
		for (Class<?> of : ofs) {
			if (!contains(roles, of))
				return false;
		}
		return true;
	}

	private boolean contains(Collection<Class<?>> roles, Class<?> of) {
		for (Class<?> type : roles) {
			if (of.isAssignableFrom(type))
				return true;
		}
		return false;
	}
}
