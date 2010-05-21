/*
 * Copyright (c) 2007-2010, James Leigh and Zepheira LLC Some rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;

/**
 * Base class for constructing behaviours from other interfaces or classes.
 * 
 * @author James Leigh
 * 
 */
public abstract class BehaviourFactory {
	public static Collection<Class<?>> findImplementations(ClassFactory cl,
			PropertyMapper mapper, Collection<Class<?>> classes,
			Set<Class<?>> bases) throws IOException {
		String services = "META-INF/services/"
				+ BehaviourFactory.class.getName();
		List<Class<?>> implementations = new ArrayList<Class<?>>();
		Enumeration<URL> resources = cl.getResources(services);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			Properties properties = new Properties();
			properties.load(url.openStream());
			for (Object key : properties.keySet()) {
				BehaviourFactory bf;
				try {
					bf = (BehaviourFactory) cl.newInstance((String) key);
					bf.setClassDefiner(cl);
					bf.setPropertyMapper(mapper);
					bf.setBaseClasses(bases);
				} catch (Exception e) {
					throw new ServiceConfigurationError(e.toString(), e);
				}
				implementations.addAll(bf.findImplementations(classes));
			}
		}
		return implementations;
	}

	protected static final String BEAN_FIELD_NAME = "_$bean";
	protected static final String CLASS_PREFIX = "object.behaviours.";
	protected ClassFactory cp;
	protected PropertyMapper properties;
	private Set<Class<?>> bases;

	public void setClassDefiner(ClassFactory definer) {
		this.cp = definer;
	}

	public void setBaseClasses(Set<Class<?>> bases) {
		this.bases = bases;
	}

	public PropertyMapper getPropertyMapper() {
		return properties;
	}

	public void setPropertyMapper(PropertyMapper mapper) {
		this.properties = mapper;
	}

	public Collection<Class<?>> findImplementations(Collection<Class<?>> classes) {
		try {
			Set<Class<?>> faces = new HashSet<Class<?>>();
			for (Class<?> i : classes) {
				faces.add(i);
				faces = getImplementingClasses(i, faces);
			}
			List<Class<?>> mappers = new ArrayList<Class<?>>();
			for (Class<?> concept : faces) {
				if (isEnhanceable(concept)) {
					mappers.addAll(findImplementations(concept));
				}
			}
			return mappers;
		} catch (ObjectCompositionException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
		}
	}

	protected abstract boolean isEnhanceable(Class<?> role)
			throws ObjectStoreConfigException;

	protected Collection<? extends Class<?>> findImplementations(
			Class<?> concept) throws Exception {
		return Collections.singleton(findBehaviour(concept));
	}

	protected void enhance(ClassTemplate cc, Class<?> role) throws Exception {
		// allow subclasses to override
	};

	protected boolean isBaseClass(Class<?> role) {
		return bases != null && bases.contains(role);
	}

	protected final Class<?> findBehaviour(Class<?> concept) throws Exception {
		String className = getJavaClassName(concept);
		synchronized (cp) {
			try {
				return cp.classForName(className);
			} catch (ClassNotFoundException e2) {
				return implement(className, concept);
			}
		}
	}

	protected Set<Class<?>> getImplementingClasses(Class<?> role,
			Set<Class<?>> implementations) {
		for (Class<?> face : role.getInterfaces()) {
			if (!implementations.contains(face)) {
				implementations.add(face);
				getImplementingClasses(face, implementations);
			}
		}
		Class<?> superclass = role.getSuperclass();
		if (superclass != null) {
			implementations.add(superclass);
			getImplementingClasses(superclass, implementations);
		}
		return implementations;
	}

	protected ClassTemplate createClassTemplate(String className, Class<?> role) {
		return cp.createClassTemplate(className);
	}

	protected ClassTemplate createBehaviourTemplate(String className,
			Class<?> concept) throws NoSuchMethodException {
		ClassTemplate cc = createClassTemplate(className, concept);
		cc.addInterface(RDFObject.class);
		cc.addInterface(RDFObjectBehaviour.class);
		addNewConstructor(cc, concept);
		addRDFObjectMethod(cc);
		addRDFObjectBehaviourMethod(cc);
		return cc;
	}

	protected boolean isOverridden(Method m) {
		if (m.getParameterTypes().length > 0)
			return false;
		if (RDFObject.GET_CONNECTION.equals(m.getName()))
			return true;
		if (RDFObject.GET_RESOURCE.equals(m.getName()))
			return true;
		if (RDFObjectBehaviour.GET_ENTITY_METHOD.equals(m.getName()))
			return true;
		return false;
	}

	private String getJavaClassName(Class<?> concept) {
		String suffix = getClass().getSimpleName().replaceAll("Factory$", "");
		return CLASS_PREFIX + concept.getName() + suffix;
	}

	private Class<?> implement(String className, Class<?> concept)
			throws Exception {
		ClassTemplate cc = createBehaviourTemplate(className, concept);
		enhance(cc, concept);
		return cp.createClass(cc);
	}

	private void addNewConstructor(ClassTemplate cc, Class<?> concept) {
		if (!concept.isInterface()) {
			try {
				concept.getConstructor(); // must have a default constructor
			} catch (NoSuchMethodException e) {
				throw new ObjectCompositionException(concept.getSimpleName()
						+ " must have a default constructor");
			}
		}
		cc.createField(ManagedRDFObject.class, BEAN_FIELD_NAME);
		cc.addConstructor(new Class<?>[] { ManagedRDFObject.class },
				BEAN_FIELD_NAME + " = (" + ManagedRDFObject.class.getName()
						+ ")$1;");
	}

	private void addRDFObjectMethod(ClassTemplate cc)
			throws ObjectCompositionException, NoSuchMethodException {
		cc.createTransientMethod(
				RDFObject.class.getDeclaredMethod(RDFObject.GET_CONNECTION))
				.code("return ").code(BEAN_FIELD_NAME).code(".").code(
						RDFObject.GET_CONNECTION).code("();").end();
		cc.createTransientMethod(
				RDFObject.class.getDeclaredMethod(RDFObject.GET_RESOURCE))
				.code("return ").code(BEAN_FIELD_NAME).code(".").code(
						RDFObject.GET_RESOURCE).code("();").end();
	}

	private void addRDFObjectBehaviourMethod(ClassTemplate cc) {
		cc.createMethod(ManagedRDFObject.class,
				RDFObjectBehaviour.GET_ENTITY_METHOD).code("return ").code(
				BEAN_FIELD_NAME).code(";").end();
	}
}
