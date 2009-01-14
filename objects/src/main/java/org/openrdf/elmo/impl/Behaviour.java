/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo.impl;

import static java.lang.reflect.Modifier.isTransient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.interceptor.InvocationContext;

import org.openrdf.elmo.annotations.intercepts;
import org.openrdf.elmo.dynacode.ClassTemplate;
import org.openrdf.elmo.dynacode.CodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Behaviour {
	private Logger logger = LoggerFactory.getLogger(Behaviour.class);
	private static final String FACTORY_SUFFIX = "Factory";

	private Class<?> javaClass;

	private ClassTemplate declaring;

	private String getterName;

	private Map<Class<?>, List<MethodFactory>> factories;

	public void setDeclaring(ClassTemplate declaring) {
		this.declaring = declaring;
	}

	public void setFactories(Map<Class<?>, List<MethodFactory>> factories) {
		this.factories = factories;
	}

	public void setJavaClass(Class<?> javaClass) {
		this.javaClass = javaClass;
	}

	public List<Method> getAroundInvoke(Method method, Class<?> face,
			ClassTemplate cc) throws Exception {
		List<Method> list = new ArrayList<Method>();
		Method jm = findInterfaceMethod(method, face);
		for (Method im : javaClass.getMethods()) {
			if (im.isAnnotationPresent(intercepts.class)) {
				intercepts it = im.getAnnotation(intercepts.class);
				if (!nameMatches(method.getName(), it, im))
					continue;
				if (!argcMatch(it.argc(), jm.getParameterTypes().length))
					continue;
				if (!argsMatch(it.parameters(), jm.getParameterTypes(), im))
					continue;
				if (!returnTypeMatches(jm, it, im.getReturnType()))
					continue;
				if (!declaredInMatches(jm, it))
					continue;
				if (isEnabled(jm, im.getDeclaringClass(), it))
					list.add(im);
			}
		}
		return list;
	}

	public String getGetterName() {
		return getterName;
	}

	public Class<?> getJavaClass() {
		return javaClass;
	}

	public boolean isMethodPresent(Method method) throws Exception {
		try {
			Class<?>[] types = method.getParameterTypes();
			Method m = javaClass.getMethod(method.getName(), types);
			if (isTransient(m.getModifiers()))
				return false;
			return !isObjectMethod(m);
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	public boolean invokeCondition(Method jm)
			throws Exception {
		for (Method im : javaClass.getMethods()) {
			if (im.isAnnotationPresent(intercepts.class)) {
				intercepts it = im.getAnnotation(intercepts.class);
				if (!nameMatches(jm.getName(), it, im))
					continue;
				if (!argcMatch(it.argc(), jm.getParameterTypes().length))
					continue;
				if (!argsMatch(it.parameters(), jm.getParameterTypes(), im))
					continue;
				if (!returnTypeMatches(jm, it, im.getReturnType()))
					continue;
				if (!declaredInMatches(jm, it))
					continue;
				if (isEnabled(jm, im.getDeclaringClass(), it))
					return true;
			}
		}
		return false;
	}

	public boolean init() throws Exception {
		try {
			getterName = "_$get" + javaClass.getSimpleName()
					+ Integer.toHexString(javaClass.hashCode());
			String fieldName = "_$" + getterName.substring(5);
			declaring.createField(javaClass, fieldName);
			CodeBuilder code = declaring.createMethod(javaClass, getterName);
			code.code("if (").code(fieldName).code(" != null){\n");
			code.code("return ").code(fieldName).code(";\n} else {\n");
			code.code("return ").code(fieldName).code(" = ($r) ");
			appendNewInstance(code);
			code.code(";\n}").end();
			return true;
		} catch (NoSuchMethodException e) {
			logger.debug(e.toString(), e);
			return false;
		}
	}

	public String toString() {
		return javaClass.getSimpleName();
	}

	private void appendNewInstance(CodeBuilder code) throws Exception {
		List<MethodFactory> list = factories.get(javaClass);
		if (list == null) {
			list = Collections.emptyList();
		}
		for (MethodFactory mf : list) {
			Class<?>[] types = mf.getMethod().getParameterTypes();
			List<Class<?>> interfaces = Arrays
					.asList(declaring.getInterfaces());
			if (types.length == 1 && !isAssignableFrom(types[0], interfaces))
				continue;
			Class<?> factory = mf.getFactoryClass();
			String name = "_$" + factory.getSimpleName()
					+ Integer.toHexString(factory.getName().hashCode())
					+ FACTORY_SUFFIX;
			CodeBuilder field = declaring.assignStaticField(factory, name);
			Method instanceMethod = mf.getInstanceMethod();
			if (instanceMethod == null) {
				field.construct(factory).end();
			} else {
				field.staticInvoke(instanceMethod).end();
			}
			code.code(name).code(".").code(mf.getMethod().getName());
			if (types.length == 0) {
				code.code("()");
			} else {
				code.code("($0)");
			}
			return;
		}
		if (!list.isEmpty()) {
			logger.debug("No factory method for: {}", javaClass.getSimpleName());
		}
		code.code("new ").code(javaClass.getName());
		if (getConstructorParameterType() == null) {
			javaClass.getDeclaredConstructor();
			code.code("()");
		} else {
			code.code("($0)");
		}
	}

	private boolean isAssignableFrom(Class<?> type,
			List<Class<?>> interfaces) {
		for (Class<?> face : interfaces) {
			if (type.isAssignableFrom(face))
				return true;
		}
		return false;
	}

	private String getConstructorParameterType() throws Exception {
		for (Constructor<?> c : javaClass.getConstructors()) {
			Class<?>[] param = c.getParameterTypes();
			if (param.length != 1)
				continue;
			if (param[0].isInterface()) {
				for (Class<?> f : declaring.getInterfaces()) {
					if (param[0].isAssignableFrom(f)) {
						return param[0].getName();
					}
				}
			} else if (Object.class.equals(param[0])) {
				return Object.class.getName();
			} else if (param[0].isAssignableFrom(declaring.getSuperclass())) {
				return param[0].getName();
			}
		}
		return null;
	}

	private boolean nameMatches(String method, intercepts it, Method im) {
		if (it.method().length() == 0)
			return im.getName().equals(method);
		return Pattern.matches(it.method(), method);
	}

	private boolean argcMatch(int argc1, int argc2) {
		return argc1 < 0 || argc2 < 0 || argc1 == argc2;
	}

	private boolean argsMatch(Class<?>[] pattern, Class<?>[] type, Method im) {
		if (pattern.length == 1 && pattern[0] == intercepts.class) {
			Class<?>[] declared = im.getParameterTypes();
			if (declared.length == 1
					&& declared[0].equals(InvocationContext.class))
				return true;
			return Arrays.asList(type).equals(Arrays.asList(declared));
		}
		return Arrays.asList(type).equals(Arrays.asList(pattern));
	}

	private boolean returnTypeMatches(Method jm, intercepts it, Class<?> rt) {
		Class<?> jrt = jm.getReturnType();
		if (it.returns() == intercepts.class)
			return rt.equals(Object.class) || isAssignableFrom(jrt, rt);
		return isAssignableFrom(jrt, it.returns());
	}

	private boolean declaredInMatches(Method jm, intercepts it) {
		if (it.declaring() == intercepts.class)
			return true;
		try {
			it.declaring().getMethod(jm.getName(), jm.getParameterTypes());
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private boolean isEnabled(Method jm, Class<?> interceptor, intercepts it)
			throws Exception {
		if (it.conditional().length() == 0)
			return true;
		Method cm = interceptor.getDeclaredMethod(it.conditional(),
				new Class[] { Method.class });
		assert cm != null : it.conditional();
		return Boolean.TRUE.equals(cm.invoke(null, new Object[] { jm }));
	}

	private boolean isAssignableFrom(Class<?> t1, Class<?> t2) {
		if (t1.isAssignableFrom(t2))
			return true;
		if (t2.isPrimitive())
			return getWrapperClass(t2).equals(t1);
		if (t1.isPrimitive())
			return getWrapperClass(t1).equals(t2);
		return false;
	}

	private Class<?> getWrapperClass(Class<?> primitiveClass) {
		if (primitiveClass.equals(Boolean.TYPE))
			return Boolean.class;
		if (primitiveClass.equals(Byte.TYPE))
			return Byte.class;
		if (primitiveClass.equals(Character.TYPE))
			return Character.class;
		if (primitiveClass.equals(Short.TYPE))
			return Short.class;
		if (primitiveClass.equals(Integer.TYPE))
			return Integer.class;
		if (primitiveClass.equals(Long.TYPE))
			return Long.class;
		if (primitiveClass.equals(Float.TYPE))
			return Float.class;
		if (primitiveClass.equals(Double.TYPE))
			return Double.class;
		if (primitiveClass.equals(Void.TYPE))
			return Void.class;
		return primitiveClass;
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}

	private Method findInterfaceMethod(Method method, Class<?> face)
			throws Exception {
		String name = method.getName();
		Class<?>[] types = method.getParameterTypes();
		Class<?> clazz = face;
		return clazz.getDeclaredMethod(name, types);
	}
}
