package org.openrdf.repository.object.composition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openrdf.model.URI;
import org.openrdf.repository.object.managers.RoleMapper;

public class ClassResolver {
	private ClassCompositor compositor;
	private RoleMapper mapper;
	private ConcurrentMap<URI, Object> individuals = new ConcurrentHashMap<URI, Object>();
	private ConcurrentMap<URI, Class<?>> singles = new ConcurrentHashMap<URI, Class<?>>();
	private ConcurrentMap<Collection<URI>, Class<?>> multiples = new ConcurrentHashMap<Collection<URI>, Class<?>>();

	public void setClassCompositor(ClassCompositor compositor) {
		this.compositor = compositor;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public Class<?> resolveEntity(URI resource, Collection<URI> types) {
		if (resource != null && mapper.isIndividualRolesPresent(resource)) {
			return resolveIndividualEntity(resource, types);
		}
		return resolveEntity(types);
	}

	public Class<?> resolveEntity(URI resource, URI type) {
		if (resource != null && mapper.isIndividualRolesPresent(resource)) {
			return resolveIndividualEntity(resource, Collections.singleton(type));
		}
		return resolveEntityType(type);
	}

	public Class<?> resolveEntity(URI resource) {
		Set<URI> emptySet = Collections.emptySet();
		return resolveEntity(resource, emptySet);
	}

	private Class<?> resolveIndividualEntity(URI resource, Collection<URI> types) {
		Object[] ar = (Object[]) individuals.get(resource);
		if (ar != null && types.equals(ar[0]))
			return (Class<?>) ar[1];
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		roles = mapper.findIndividualRoles(resource, roles);
		roles = mapper.findRoles(types, roles);
		Class<?> proxy = compositor.resolveRoles(roles);
		individuals.put(resource, new Object[] { types, proxy });
		return proxy;
	}

	private Class<?> resolveEntityType(URI type) {
		Class<?> proxy = singles.get(type);
		if (proxy != null)
			return proxy;
		proxy = compositor.resolveRoles(mapper.findRoles(type));
		singles.putIfAbsent(type, proxy);
		return proxy;
	}

	private Class<?> resolveEntity(Collection<URI> types) {
		Class<?> proxy = multiples.get(types);
		if (proxy != null)
			return proxy;
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		proxy = compositor.resolveRoles(mapper.findRoles(types, roles));
		multiples.putIfAbsent(types, proxy);
		return proxy;
	}

}
