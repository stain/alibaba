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
package org.openrdf.repository.object.composition;

import static java.lang.reflect.Modifier.isAbstract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openrdf.model.URI;
import org.openrdf.repository.object.composition.helpers.ClassCompositor;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find a proxy class that can be used for a set of rdf:types.
 * 
 * @author James Leigh
 *
 */
public class ClassResolver {
	private static final String PKG_PREFIX = "object.proxies._";
	private static final String CLASS_PREFIX = "_EntityProxy";
	private Logger logger = LoggerFactory.getLogger(ClassResolver.class);
	private PropertyMapperFactory propertyResolver;
	private AbstractClassFactory abstractResolver;
	private BehaviourFactory[] otherResolvers;
	private ClassFactory cp;
	private Collection<Class<?>> baseClassRoles;
	private RoleMapper mapper;
	private Class<?> blank;
	private ConcurrentMap<Collection<URI>, Class<?>> multiples = new ConcurrentHashMap<Collection<URI>, Class<?>>();

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public void setInterfaceBehaviourResolver(PropertyMapperFactory loader) {
		this.propertyResolver = loader;
	}

	public void setAbstractBehaviourResolver(AbstractClassFactory loader) {
		this.abstractResolver = loader;
	}

	public void setOtherBehaviourFactory(BehaviourFactory... loader) {
		this.otherResolvers = loader;
	}

	public void setClassDefiner(ClassFactory definer) {
		this.cp = definer;
	}

	public void setBaseClassRoles(Collection<Class<?>> baseClassRoles) {
		this.baseClassRoles = new ArrayList<Class<?>>(baseClassRoles.size());
		for (Class<?> base : baseClassRoles) {
			try {
				// ensure the base class has a default constructor
				base.getConstructor();
				this.baseClassRoles.add(base);
			} catch (NoSuchMethodException e) {
				logger.warn("Concept will only be mergable: {}", base);
			}
		}
	}

	public void init() {
		Set<URI> emptySet = Collections.emptySet();
		blank = resolveBlankEntity(emptySet);
	}

	public Class<?> resolveBlankEntity() {
		return blank;
	}

	public Class<?> resolveBlankEntity(Collection<URI> types) {
		Class<?> proxy = multiples.get(types);
		if (proxy != null)
			return proxy;
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		proxy = resolveRoles(mapper.findRoles(types, roles));
		multiples.putIfAbsent(types, proxy);
		return proxy;
	}

	public Class<?> resolveEntity(URI resource) {
		if (resource != null && mapper.isIndividualRolesPresent(resource))
			return resolveIndividualEntity(resource, Collections.EMPTY_SET);
		return resolveBlankEntity();
	}

	public Class<?> resolveEntity(URI resource, Collection<URI> types) {
		if (resource != null && mapper.isIndividualRolesPresent(resource))
			return resolveIndividualEntity(resource, types);
		return resolveBlankEntity(types);
	}

	private Class<?> resolveIndividualEntity(URI resource, Collection<URI> types) {
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		roles = mapper.findIndividualRoles(resource, roles);
		roles = mapper.findRoles(types, roles);
		return resolveRoles(roles);
	}

	private Class<?> resolveRoles(Collection<Class<?>> roles) {
		try {
			String className = getJavaClassName(roles);
			return getComposedBehaviours(className, roles);
		} catch (Exception e) {
			List<String> roleNames = new ArrayList<String>();
			for (Class<?> f : roles) {
				roleNames.add(f.getSimpleName());
			}
			throw new ObjectCompositionException(e.getMessage()
					+ " for entity with roles: " + roleNames, e);
		}
	}

	private Class<?> getComposedBehaviours(String className,
			Collection<Class<?>> roles) throws Exception {
		try {
			return Class.forName(className, true, cp);
		} catch (ClassNotFoundException e) {
			synchronized (cp) {
				try {
					return Class.forName(className, true, cp);
				} catch (ClassNotFoundException e1) {
					return composeBehaviours(className, roles);
				}
			}
		}
	}

	private Class<?> composeBehaviours(String className,
			Collection<Class<?>> roles) throws Exception {
		List<Class<?>> types = new ArrayList<Class<?>>(roles.size());
		types.addAll(roles);
		types = removeSuperClasses(types);
		ClassCompositor cc = new ClassCompositor(className, types.size());
		cc.setClassFactory(cp);
		cc.setPropertyResolver(propertyResolver);
		cc.setRoleMapper(mapper);
		Set<Class<?>> abstracts = new LinkedHashSet<Class<?>>(types.size());
		Set<Class<?>> concretes = new LinkedHashSet<Class<?>>(types.size());
		Set<Class<?>> bases = new LinkedHashSet<Class<?>>();
		Class<?> baseClass = Object.class;
		for (Class<?> role : types) {
			if (role.isInterface()) {
				cc.addInterface(role);
			} else if (baseClassRoles.contains(role)) {
				if (baseClass != null && baseClass.isAssignableFrom(role)) {
					baseClass = role;
				} else if (!role.equals(baseClass)) {
					baseClass = null;
				}
				bases.add(role);
			} else if (isAbstract(role.getModifiers())) {
				abstracts.add(role);
			} else {
				concretes.add(role);
			}
		}
		if (baseClass == null) {
			logger.warn("Cannot compose multiple concept classes: " + types);
		} else {
			cc.setBaseClass(baseClass);
		}
		cc.addAllBehaviours(concretes);
		concretes.addAll(bases);
		cc.addAllBehaviours(abstractResolver.findImplementations(abstracts));
		cc.addAllBehaviours(propertyResolver.findImplementations(concretes));
		cc.addAllBehaviours(propertyResolver.findImplementations(abstracts));
		cc.addAllBehaviours(propertyResolver.findImplementations(cc.getInterfaces()));
		for (BehaviourFactory bf : otherResolvers) {
			cc.addAllBehaviours(bf.findImplementations(concretes));
			cc.addAllBehaviours(bf.findImplementations(abstracts));
			cc.addAllBehaviours(bf.findImplementations(cc.getInterfaces()));
		}
		return cc.compose();
	}

	@SuppressWarnings("unchecked")
	private List<Class<?>> removeSuperClasses(List<Class<?>> classes) {
		for (int i = classes.size() - 1; i >= 0; i--) {
			Class<?> c = classes.get(i);
			for (int j = classes.size() - 1; j >= 0; j--) {
				Class<?> d = classes.get(j);
				if (i != j && c.isAssignableFrom(d)
						&& c.isInterface() == d.isInterface()) {
					classes.remove(i);
					break;
				}
			}
		}
		return classes;
	}

	private String getJavaClassName(Collection<Class<?>> javaClasses) {
		String phex = packagesToHexString(javaClasses);
		String chex = classesToHexString(javaClasses);
		return PKG_PREFIX + phex + "." + CLASS_PREFIX + chex;
	}

	private String packagesToHexString(Collection<Class<?>> javaClasses) {
		TreeSet<String> names = new TreeSet<String>();
		for (Class<?> clazz : javaClasses) {
			if (clazz.getPackage() != null) {
				names.add(clazz.getPackage().getName());
			}
		}
		return toHexString(names);
	}

	private String classesToHexString(Collection<Class<?>> javaClasses) {
		TreeSet<String> names = new TreeSet<String>();
		for (Class<?> clazz : javaClasses) {
			names.add(clazz.getName());
		}
		return toHexString(names);
	}

	private String toHexString(TreeSet<String> names) {
		long hashCode = 0;
		for (String name : names) {
			hashCode = 31 * hashCode + name.hashCode();
		}
		return Long.toHexString(hashCode);
	}

}
