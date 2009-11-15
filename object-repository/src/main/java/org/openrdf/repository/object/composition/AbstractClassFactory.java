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
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isProtected;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;

/**
 * Creates subclasses of abstract behaviours that can be instaniated.
 * 
 * @author James Leigh
 * 
 */
public class AbstractClassFactory extends BehaviourFactory {

	@Override
	protected Set<Class<?>> getImplementingClasses(Class<?> role,
			Set<Class<?>> implementations) {
		// don't consider interfaces or super classes
		return implementations;
	}

	@Override
	protected ClassTemplate createClassTemplate(String className, Class<?> role) {
		ClassTemplate cc = cp.createClassTemplate(className, role);
		cc.copyAnnotationsFrom(role);
		return cc;
	}

	protected boolean isEnhanceable(Class<?> role)
			throws ObjectStoreConfigException {
		return !role.isInterface() && isAbstract(role.getModifiers())
				&& !isBaseClass(role);
	}

	protected void enhance(ClassTemplate cc, Class<?> c) throws Exception {
		for (Method m : getMethods(c)) {
			if (isFinal(m.getModifiers()))
				continue;
			if (!isAbstract(m.getModifiers()))
				continue;
			Class<?> r = m.getReturnType();
			Class<?>[] types = m.getParameterTypes();
			CodeBuilder code = cc.createTransientMethod(m);
			boolean isInterface = m.getDeclaringClass().isInterface();
			if (!isInterface) {
				code.code("try {");
			}
			if (!Void.TYPE.equals(r)) {
				code.code("return ($r) ");
			}
			if (isInterface) {
				code.code("(").castObject(BEAN_FIELD_NAME,
						m.getDeclaringClass());
				code.code(").").code(m.getName()).code("($$);");
			} else {
				code.code(BEAN_FIELD_NAME).code(".getClass().getMethod(");
				code.insert(m.getName()).code(", ").insert(types).code(")")
						.code(".invoke(");
				code.code(BEAN_FIELD_NAME).code(", $args);");
			}
			if (!isInterface) {
				code.code("} catch (").code(
						InvocationTargetException.class.getName());
				code.code(" e) {throw e.getCause();}");
			}
			code.end();
		}
	}

	private Collection<Method> getMethods(Class<?> c) {
		List<Method> methods = new ArrayList<Method>();
		methods.addAll(Arrays.asList(c.getMethods()));
		HashMap<Object, Method> map = new HashMap<Object, Method>();
		Map<Object, Method> pms = getProtectedMethods(c, map);
		methods.addAll(pms.values());
		return methods;
	}

	private Map<Object, Method> getProtectedMethods(Class<?> c,
			Map<Object, Method> methods) {
		if (c == null)
			return methods;
		for (Method m : c.getDeclaredMethods()) {
			if (isProtected(m.getModifiers())) {
				Object types = Arrays.asList(m.getParameterTypes());
				Object key = Arrays.asList(m.getName(), types);
				if (!methods.containsKey(key)) {
					methods.put(key, m);
				}
			}
		}
		return getProtectedMethods(c.getSuperclass(), methods);
	}

}
