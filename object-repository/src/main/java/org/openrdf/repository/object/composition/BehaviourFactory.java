/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;

/**
 * Base class for constructing behaviours from other interfaces or classes.
 * 
 * @author James Leigh
 * 
 */
public abstract class BehaviourFactory {

	protected static final String BEAN_FIELD_NAME = "_$bean";

	protected ClassFactory cp;

	protected PropertyMapper properties;

	public void setClassDefiner(ClassFactory definer) {
		this.cp = definer;
	}

	public void setPropertyMapper(PropertyMapper mapper) {
		this.properties = mapper;
	}

	public Collection<Class<?>> findImplementations(
			Collection<Class<?>> interfaces) {
		try {
			Set<Class<?>> faces = new HashSet<Class<?>>();
			for (Class<?> i : interfaces) {
				faces.add(i);
				faces = getInterfaces(i, faces);
			}
			List<Class<?>> mappers = new ArrayList<Class<?>>();
			for (Class<?> concept : faces) {
				if (isEnhanceable(concept)) {
					mappers.add(findBehaviour(concept));
				}
			}
			return mappers;
		} catch (ObjectCompositionException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
		}
	}

	protected abstract String getJavaClassName(Class<?> concept);

	protected abstract void enhance(ClassTemplate cc, Class<?> concept)
			throws Exception;

	protected Class<?> findBehaviour(Class<?> concept) throws Exception {
		String className = getJavaClassName(concept);
		try {
			return Class.forName(className, true, cp);
		} catch (ClassNotFoundException e1) {
			synchronized (cp) {
				try {
					return Class.forName(className, true, cp);
				} catch (ClassNotFoundException e2) {
					return implement(className, concept);
				}
			}
		}
	}

	protected abstract boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException;

	private Class<?> implement(String className, Class<?> concept)
			throws Exception {
		ClassTemplate cc = cp.createClassTemplate(className);
		cc.addInterface(RDFObject.class);
		addNewConstructor(cc);
		addRDFObjectMethod(cc);
		enhance(cc, concept);
		return cp.createClass(cc);
	}

	private Set<Class<?>> getInterfaces(Class<?> concept,
			Set<Class<?>> interfaces) throws NotFoundException {
		for (Class<?> face : concept.getInterfaces()) {
			if (!interfaces.contains(face)) {
				interfaces.add(face);
				getInterfaces(face, interfaces);
			}
		}
		Class<?> superclass = concept.getSuperclass();
		if (superclass != null) {
			interfaces.add(superclass);
			getInterfaces(superclass, interfaces);
		}
		return interfaces;
	}

	private void addNewConstructor(ClassTemplate cc) throws NotFoundException,
			CannotCompileException {
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
}
