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
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.HierarchicalRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleMapper {
	private ValueFactory vf;
	private Logger logger = LoggerFactory.getLogger(RoleMapper.class);
	private HierarchicalRoleMapper roleMapper = new HierarchicalRoleMapper();
	private Map<URI, List<Class<?>>> instances = new ConcurrentHashMap<URI, List<Class<?>>>(
			256);
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
		return classes;
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

	public boolean isIndividualRolesPresent(URI instance) {
		return !instances.isEmpty() && instances.containsKey(instance);
	}

	public boolean isTypeRecorded(URI type) {
		return roleMapper.isTypeRecorded(type);
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

	public void addAnnotation(Class<?> annotation) {
		if (!annotation.isAnnotationPresent(rdf.class))
			throw new IllegalArgumentException("@rdf annotation required in "
					+ annotation.getSimpleName());
		String uri = annotation.getAnnotation(rdf.class).value();
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
		assertNotConcept(role);
		boolean hasType = false;
		for (Class<?> face : role.getInterfaces()) {
			boolean recorded = recordRole(role, face, null, false, false);
			if (recorded && hasType) {
				throw new ObjectStoreConfigException(role.getSimpleName()
						+ " can only implement one concept");
			} else {
				hasType = true;
			}
		}
		if (!hasType)
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " must implement a concept or mapped explicitly");
	}

	public void addBehaviour(Class<?> role, URI type)
			throws ObjectStoreConfigException {
		assertNotConcept(role);
		recordRole(role, null, type, false, false);
		for (Class<?> face : role.getInterfaces()) {
			if (recordRole(role, face, null, false, false))
				throw new ObjectStoreConfigException(
						role.getSimpleName()
								+ " cannot implement concept interfaces when mapped explicitly");
		}
	}

	private void assertNotConcept(Class<?> role)
			throws ObjectStoreConfigException {
		if (isAnnotationPresent(role))
			throw new ObjectStoreConfigException(role.getSimpleName()
					+ " cannot have a concept annotation");
		for (Method method : role.getDeclaredMethods()) {
			if (isAnnotationPresent(method)
					&& method.getName().startsWith("get"))
				throw new ObjectStoreConfigException(role.getSimpleName()
						+ " cannot have a property annotation");
		}
	}

	private boolean isAnnotationPresent(AnnotatedElement role)
			throws ObjectStoreConfigException {
		String pkg = rdf.class.getPackage().getName();
		for (Annotation ann : role.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (intercepts.class.equals(type))
				continue;
			if (pkg.equals(type.getPackage().getName()))
				return true;
		}
		return false;
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
						&& ann.annotationType().isAnnotationPresent(rdf.class)) {
					addAnnotation(ann.annotationType());
					name = findAnnotation(ann.annotationType());
				}
				if (name == null)
					continue;
				Object value = ann.getClass().getMethod("value").invoke(ann);
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
						for (Class<?> concept : findRoles(vf
								.createURI((String) value))) {
							complements.put(role, concept);
							recorded = true;
						}
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
							ofs.addAll(findRoles(vf.createURI((String) v)));
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
							recorded |= recordRole(role, concept, null,
									isConcept, true);
						} else {
							for (Class<?> concept : findRoles(vf
									.createURI((String) v))) {
								if (!role.equals(concept)) {
									recorded |= recordRole(role, concept, null,
											isConcept, true);
								}
							}
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
		if (elm.isAnnotationPresent(rdf.class)) {
			String value = elm.getAnnotation(rdf.class).value();
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
