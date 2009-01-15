package org.openrdf.repository.object.managers;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.equivalent;
import org.openrdf.repository.object.annotations.factory;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.exceptions.ElmoInitializationException;
import org.openrdf.repository.object.managers.helpers.ComplexMapper;
import org.openrdf.repository.object.managers.helpers.HierarchicalRoleMapper;

public class RoleMapper {
	private URIFactory vf;

	private HierarchicalRoleMapper roleMapper;

	private Map<URI, List<Class<?>>> instances = new ConcurrentHashMap<URI, List<Class<?>>>(256);

	private ComplexMapper additional;

	private Set<Class<?>> conceptClasses = new HashSet<Class<?>>();

	private Set<Class<?>> conceptOnlyClasses = new HashSet<Class<?>>();

	public void setHierarchicalRoleMapper(HierarchicalRoleMapper roleMapper) {
		this.roleMapper = roleMapper;
	}

	public void setComplexMapper(ComplexMapper additional) {
		this.additional = additional;
	}

	public void setURIFactory(URIFactory vf) {
		this.vf = vf;
		roleMapper.setURIFactory(vf);
	}

	public Collection<Class<?>> getConceptClasses() {
		return conceptClasses;
	}

	public Collection<Class<?>> getConceptOnlyClasses() {
		return conceptOnlyClasses;
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
		return additional.findAdditonalRoles(roleMapper.findRoles(type));
	}

	public Collection<Class<?>> findRoles(Collection<URI> types,
			Collection<Class<?>> roles) {
		return additional.findAdditonalRoles(roleMapper.findRoles(types, roles));
	}

	public Collection<Class<?>> findAdditionalRoles(Collection<Class<?>> classes) {
		return additional.findAdditonalRoles(classes);
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

	public void addFactory(Class<?> javaClass) {
		assertIsFactory(javaClass);
		recordRole(javaClass, javaClass, false);
	}

	public void addFactory(Class<?> javaClass, String type) {
		assertIsFactory(javaClass);
		URI uri = vf.createURI(type);
		recordRole(javaClass, javaClass, uri, false);
	}

	private void assertIsFactory(Class<?> javaClass) {
		boolean hasFactory = false;
		for (Method method : javaClass.getMethods()) {
			if (method.isAnnotationPresent(factory.class)) {
				hasFactory = true;
			}
		}
		if (!hasFactory)
			throw new IllegalArgumentException("Class has no factory methods");
	}

	public void addConcept(Class<?> role) {
		if (!role.isInterface()) {
			if (findType(role) == null) {
				conceptOnlyClasses.add(role);
			}
			conceptClasses.add(role);
		}
		recordRole(role, role, true);
	}

	public void addConcept(Class<?> role, String type) {
		if (!role.isInterface()) {
			if (findType(role) == null) {
				conceptOnlyClasses.add(role);
			}
			conceptClasses.add(role);
		}
		recordRole(role, role, vf.createURI(type), true);
	}

	public void addBehaviour(Class<?> role) {
		conceptOnlyClasses.remove(role);
		recordRole(role, role, false);
	}

	public void addBehaviour(Class<?> role, String type) {
		conceptOnlyClasses.remove(role);
		recordRole(role, role, vf.createURI(type), false);
	}

	private void recordRole(Class<?> role, Class<?> elm, boolean concept) {
		recordRole(role, elm, null, concept, true);
	}

	private boolean recordRole(Class<?> role, Class<?> elm, URI rdfType, boolean concept) {
		return recordRole(role, elm, rdfType, concept, false);
	}

	private boolean recordRole(Class<?> role, Class<?> elm, URI rdfType, boolean concept, boolean base) {
		boolean isRecorded = recordExplicitRoles(role, elm, rdfType, concept, base);
		recordAliases(role, elm, concept);
		return isRecorded;
	}

	private boolean recordExplicitRoles(Class<?> role, Class<?> elm, URI rdfType, boolean concept, boolean base) {
		boolean defaultType = elm.isAnnotationPresent(rdf.class);
		boolean complement = elm.isAnnotationPresent(complementOf.class);
		boolean intersec = elm.isAnnotationPresent(intersectionOf.class);
		boolean one = elm.isAnnotationPresent(oneOf.class);
		boolean annotated = defaultType;
		annotated = annotated || complement || intersec || one;
		URI defType = findDefaultType(role, elm);
		boolean hasType = annotated;
		if (defType != null) {
			if (concept) {
				roleMapper.recordConcept(role, defType);
			} else {
				roleMapper.recordBehaviour(role, defType);
			}
			hasType = true;
		}
		if (rdfType != null) {
			if (concept) {
				roleMapper.recordConcept(role, rdfType);
			} else {
				roleMapper.recordBehaviour(role, rdfType);
			}
			hasType = true;
		}
		if (!hasType) {
			for (Class<?> face : elm.getInterfaces()) {
				hasType |= recordRole(role, face, null, concept);
			}
		}
		if (!hasType) {
			for (Method method : elm.getMethods()) {
				if (method.isAnnotationPresent(factory.class)) {
					Class<?> type = method.getReturnType();
					hasType |= recordRole(role, type, null, concept);
				}
			}
		}
		if (!hasType && base) {
			throw new ElmoInitializationException(role.getSimpleName() + " does not have an RDF type mapping");
		}
		additional.recordRole(role, elm);
		recordOneOf(role, elm);
		return hasType;
	}

	private void recordOneOf(Class<?> role, Class<?> elm) {
		if (elm.isAnnotationPresent(oneOf.class)) {
			oneOf ann = elm.getAnnotation(oneOf.class);
			for (String instance : ann.value()) {
				URI uri = vf.createURI(instance);
				List<Class<?>> list = instances.get(uri);
				if (list == null) {
					list = new CopyOnWriteArrayList<Class<?>>();
					instances.put(uri, list);
				}
				list.add(role);
			}
		}
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

	private void recordAliases(Class<?> role, Class<?> elm, boolean concept) {
		if (elm.isAnnotationPresent(equivalent.class)) {
			String[] uris = elm.getAnnotation(equivalent.class).value();
			for (int i = 0; i < uris.length; i++) {
				URI eqType = vf.createURI(uris[i]);
				recordExplicitRoles(role, elm, eqType, concept, false);
			}
		}
	}
}
