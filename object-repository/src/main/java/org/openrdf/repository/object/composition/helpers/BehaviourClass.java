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
package org.openrdf.repository.object.composition.helpers;

import static java.lang.reflect.Modifier.isTransient;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.openrdf.repository.object.annotations.parameters;
import org.openrdf.repository.object.annotations.subMethodOf;
import org.openrdf.repository.object.composition.ClassTemplate;
import org.openrdf.repository.object.composition.CodeBuilder;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BehaviourClass {
	private Logger logger = LoggerFactory.getLogger(BehaviourClass.class);

	private Class<?> javaClass;

	private ClassTemplate declaring;

	private String getterName;

	public void setDeclaring(ClassTemplate declaring) {
		this.declaring = declaring;
	}

	public void setJavaClass(Class<?> javaClass) {
		this.javaClass = javaClass;
	}

	public String getGetterName() {
		return getterName;
	}

	public Class<?> getJavaClass() {
		return javaClass;
	}

	public boolean isMethodPresent(Method method) throws Exception {
		return getMethod(method) != null;
	}

	public boolean isSubMethodOf(BehaviourClass b1, Method method) throws Exception {
		Method m = getMethod(method);
		subMethodOf ann = m.getAnnotation(subMethodOf.class);
		if (ann != null) {
			for (Class<?> c : ann.value()) {
				if (c.equals(b1.getJavaClass()))
					return true;
			}
		}
		return false;
	}

	public boolean isMessage(Method method) throws Exception {
		return getMethod(method).isAnnotationPresent(parameters.class);
	}

	public Method getMethod(Method method) throws Exception {
		try {
			Class<?>[] types = method.getParameterTypes();
			Method m = javaClass.getMethod(method.getName(), types);
			if (!isTransient(m.getModifiers()) && !isObjectMethod(m))
				return m;
		} catch (NoSuchMethodException e) {
			// look at @parameters
		}
		Class<?>[] type = method.getParameterTypes();
		for (Method m : javaClass.getMethods()) {
			if (m.getName().equals(method.getName())) {
				parameters ann = m.getAnnotation(parameters.class);
				if (ann != null && Arrays.equals(ann.value(), type))
					return m;
			}
		}
		return null;
	}

	public boolean init() throws Exception {
		try {
			getterName = "_$get" + javaClass.getSimpleName()
					+ Integer.toHexString(javaClass.hashCode());
			String fieldName = "_$" + getterName.substring(5);
			declaring.createField(javaClass, fieldName);
			CodeBuilder code = declaring.createPrivateMethod(javaClass, getterName);
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
		code.code("new ").code(javaClass.getName());
		try {
			javaClass.getConstructor(ManagedRDFObject.class);
			code.code("($0)");
		} catch (NoSuchMethodException e) {
			javaClass.getConstructor();
			code.code("()");
		}
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}
}
