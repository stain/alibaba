/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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

import static org.openrdf.repository.object.composition.PropertyMapperFactory.getLoadedMethod;
import static org.openrdf.repository.object.composition.PropertyMapperFactory.getMapperClassNameFor;
import static org.openrdf.repository.object.composition.PropertyMapperFactory.getReadMethod;
import static org.openrdf.repository.object.composition.PropertyMapperFactory.getWriteMethod;
import static org.openrdf.repository.object.composition.helpers.ClassCompositor.getPrivateBehaviourMethod;
import static org.openrdf.repository.object.composition.helpers.InvocationMessageContext.PROCEED;
import static org.openrdf.repository.object.traits.RDFObjectBehaviour.GET_ENTITY_METHOD;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javassist.NotFoundException;

import org.openrdf.annotations.Iri;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.annotations.Precedes;
import org.openrdf.repository.object.composition.helpers.InvocationMessageContext;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;

/**
 * Fields that have the {@link Iri} annotation are prepopulated with
 * values from the Repository.
 * 
 * @author James Leigh
 * 
 */
public class FieldPopulaterFactory extends BehaviourFactory {
	private int count = 0;

	protected boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException {
		if (concept.equals(Object.class))
			return false;
		if (!properties.findFields(concept).isEmpty())
			return true;
		return false;
	}

	@Override
	protected Collection<? extends Class<?>> findImplementations(Class<?> role)
			throws Exception {
		List<Class<?>> behaviours = new ArrayList<Class<?>>();
		for (Method method : role.getDeclaredMethods()) {
			ClassTemplate t = cp.loadClassTemplate(role);
			Set<Field> fieldsRead = getMappedFieldsRead(t, method);
			Set<Field> fieldsWriten = getMappedFieldsWritten(t, method);
			if (!fieldsRead.isEmpty() || !fieldsWriten.isEmpty()) {
				behaviours.add(findBehaviour(method, fieldsRead, fieldsWriten));
			}
		}
		return behaviours;
	}

	private Set<Field> getMappedFieldsRead(ClassTemplate t, Method method)
			throws NotFoundException {
		Set<Field> fields = t.getFieldsRead(method);
		Iterator<Field> iter = fields.iterator();
		while (iter.hasNext()) {
			Field field = iter.next();
			if (!properties.isMappedField(field)) {
				iter.remove();
			}
		}
		return fields;
	}

	private Set<Field> getMappedFieldsWritten(ClassTemplate t, Method method)
			throws NotFoundException {
		Set<Field> fields = t.getFieldsWritten(method);
		Iterator<Field> iter = fields.iterator();
		while (iter.hasNext()) {
			Field field = iter.next();
			if (!properties.isMappedField(field)) {
				iter.remove();
			}
		}
		return fields;
	}

	private Class<?> findBehaviour(Method method, Set<Field> fieldsRead,
			Set<Field> fieldsWriten) throws Exception {
		String className = getJavaClassName(method.getDeclaringClass(), method);
		synchronized (cp) {
			try {
				return Class.forName(className, true, cp);
			} catch (ClassNotFoundException e2) {
				return implement(className, method, fieldsRead, fieldsWriten);
			}
		}
	}

	private String getJavaClassName(Class<?> concept, Method method) {
		String suffix = getClass().getSimpleName().replaceAll("Factory$", "");
		String m = "$" + method.getName() + toHexString(method);
		return CLASS_PREFIX + concept.getName() + m + suffix;
	}

	private Class<?> implement(String className, Method method,
			Set<Field> fieldsRead, Set<Field> fieldsWriten) throws Exception {
		Class<?> role = method.getDeclaringClass();
		ClassTemplate cc = createBehaviourTemplate(className, role);
		cc.addAnnotation(Precedes.class, role);
		createInvokePrivate(cc);
		interceptMethod(method, fieldsRead, fieldsWriten, cc);
		return cp.createClass(cc);
	}

	private void createInvokePrivate(ClassTemplate cc) {
		MethodBuilder body = cc.createPrivateMethod(Object.class, "invokePrivateMethod", String.class);
		body.declareObject(Method.class, "method");
		body.code(GET_ENTITY_METHOD);
		body.code("().getClass().getDeclaredMethod($1, null)").semi();
		body.code("method.setAccessible(true)").semi();
		body.code("return method.invoke(");
		body.code(GET_ENTITY_METHOD).code("(), null)").semi();
		body.end();
	}

	private void interceptMethod(Method method, Set<Field> fieldsRead,
			Set<Field> fieldsWriten, ClassTemplate cc) throws Exception {
		Class<?> type = method.getReturnType();
		MethodBuilder body = cc.createMethod(type, method.getName(),
				InvocationMessageContext.selectMessageType(type));
		body.ann(ParameterTypes.class, method.getParameterTypes());
		body.code("try {\n");
		if (!fieldsRead.isEmpty()) {
			body.code("try {");
			int count = 0;
			for (Field field : fieldsRead) {
				populateField(field, body, cc, count++);
			}
			body.code("} catch (").code(
					InvocationTargetException.class.getName());
			body.code(" e) {throw e.getCause();}");
		}
		boolean voidReturnType = type.equals(Void.TYPE);
		boolean primitiveReturnType = type.isPrimitive();
		if (voidReturnType) {
			body.code("$1." + PROCEED + "()").semi();
		} else if (primitiveReturnType) {
			body.code("return $1." + PROCEED + "()").semi();
		} else {
			body.code("return ").cast(type).code(
					"$1." + PROCEED+ "()").semi();
		}
		body.code("} finally {\n");
		if (!fieldsWriten.isEmpty()) {
			body.code("try {");
			int count = 0;
			for (Field field : fieldsWriten) {
				saveFieldValue(field, body, cc, count++);
			}
			body.code("} catch (").code(
					InvocationTargetException.class.getName());
			body.code(" e) {throw e.getCause();}");
		}
		body.code("}\n");
		body.end();
	}

	private void populateField(Field field, CodeBuilder body, ClassTemplate cc, int count)
			throws Exception {
		Class<?> type = field.getType();
		body.code("if (!(").cast(Boolean.class);
		call(body, cc, field.getDeclaringClass(), getLoadedMethod(field));
		body.code(").booleanValue()) {\n");
		String fieldVar = field.getName() + "Field" + count;
		body.declareObject(Field.class, fieldVar);
		body.insert(field.getDeclaringClass());
		body.code(".getDeclaredField(\"");
		body.code(field.getName()).code("\")").semi();
		body.code(fieldVar).code(".setAccessible(true)").semi();
		body.code(fieldVar).code(".set");
		if (type.isPrimitive()) {
			String tname = type.getName();
			body.code(tname.substring(0, 1).toUpperCase());
			body.code(tname.substring(1));
			body.code("(").code(GET_ENTITY_METHOD).code("(), (").castObject(type);
			call(body, cc, field.getDeclaringClass(), getReadMethod(field));
			body.code(").").code(type.getName()).code("Value())");
		} else {
			body.code("(").code(GET_ENTITY_METHOD).code("(), ").cast(type);
			call(body, cc, field.getDeclaringClass(), getReadMethod(field));
			body.code(")");
		}
		body.semi();
		body.code("}\n");
	}

	private void call(CodeBuilder code, ClassTemplate cc, Class<?> concept, String method) {
		invoke(code, cc, concept, method, "null");
	}

	private void saveFieldValue(Field field, CodeBuilder body, ClassTemplate cc, int count)
			throws Exception {
		body.code("java.lang.Object[] arg = new java.lang.Object[1]").semi();
		String fieldVar = field.getName() + "Field" + count;
		body.declareObject(Field.class, fieldVar);
		body.insert(field.getDeclaringClass());
		body.code(".getDeclaredField(");
		body.insert(field.getName()).code(")").semi();
		body.code(fieldVar).code(".setAccessible(true)").semi();
		body.code("arg[0] = ");
		if (field.getType().isPrimitive()) {
			body.valueOf(field.getType());
		}
		body.code("(").code(fieldVar).code(".get");
		if (field.getType().isPrimitive()) {
			String tname = field.getType().getName();
			body.code(tname.substring(0, 1).toUpperCase());
			body.code(tname.substring(1));
		}
		body.code("(").code(GET_ENTITY_METHOD).code("()))").semi();
		invoke(body, cc, field.getDeclaringClass(), getWriteMethod(field), "arg", field.getType());
		body.semi();
	}

	private void invoke(CodeBuilder body, ClassTemplate cc, Class<?> concept, String name, String arg,
			Class<?>... parameters) {
		String fieldName = "method" + (++count);
		CodeBuilder m = cc.assignStaticField(Method.class, fieldName);
		m.insertObjectClass(getMapperClassNameFor(concept)).code(".getDeclaredMethod(");
		m.insert(name).code(", ");
		m.insert(parameters).code(")").semi();
		m.code(fieldName).code(".setAccessible(true)").end();
		body.code(fieldName).code(".invoke(invokePrivateMethod(");
		body.insert(getPrivateBehaviourMethod(getMapperClassNameFor(concept)));
		body.code("), ").code(arg).code(")");
	}

	private String toHexString(Method m) {
		Class<?> r = m.getReturnType();
		List<Class<?>> p = Arrays.asList(m.getParameterTypes());
		int code = 31 * p.hashCode() + r.hashCode();
		return Integer.toHexString(Math.abs(code));
	}
}
