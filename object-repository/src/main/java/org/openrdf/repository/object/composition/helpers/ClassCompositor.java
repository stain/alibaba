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
package org.openrdf.repository.object.composition.helpers;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isTransient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassTemplate;
import org.openrdf.repository.object.composition.CodeBuilder;
import org.openrdf.repository.object.composition.PropertyMapperFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * This class takes a collection of roles (interfaces or classes) and uses
 * composition to combine this into a single class.
 * 
 * @author James Leigh
 * 
 */
public class ClassCompositor {
	private static Set<String> special = new HashSet<String>(Arrays.asList(
			"groovy.lang.GroovyObject", RDFObjectBehaviour.class.getName()));
	private ClassFactory cp;
	private PropertyMapperFactory propertyResolver;
	private RoleMapper mapper;
	private String className;
	private Class<?> baseClass = Object.class;
	private Set<Class<?>> interfaces;
	private Set<Class<?>> javaClasses;
	private Collection<Method> methods;
	private Map<String, Method> namedMethods;
	private List<Class<?>> behaviours;
	private ClassTemplate cc;

	public ClassCompositor(String className, int size) {
		this.className = className;
		interfaces = new LinkedHashSet<Class<?>>(size);
		javaClasses = new LinkedHashSet<Class<?>>(size);
	}

	public void setClassFactory(ClassFactory cp) {
		this.cp = cp;
	}

	public void setPropertyResolver(PropertyMapperFactory propertyResolver) {
		this.propertyResolver = propertyResolver;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public void setBaseClass(Class<?> baseClass) {
		this.baseClass = baseClass;
	}

	public Set<Class<?>> getInterfaces() {
		return interfaces;
	}

	public void addInterface(Class<?> iface) {
		this.interfaces.add(iface);
	}

	public void addAllBehaviours(Collection<Class<?>> javaClasses) {
		this.javaClasses.addAll(javaClasses);
	}

	public Class<?> compose() throws Exception {
		cc = cp.createClassTemplate(className, baseClass);
		for (Class<?> clazz : javaClasses) {
			addInterfaces(clazz);
		}
		for (Class<?> face : interfaces) {
			cc.addInterface(face);
		}
		behaviours = new ArrayList<Class<?>>();
		for (Class<?> clazz : javaClasses) {
			if (addBehaviour(clazz)) {
				behaviours.add(clazz);
			}
		}
		if (baseClass != null && !Object.class.equals(javaClasses)) {
			javaClasses.add(baseClass);
		}
		methods = getMethods();
		namedMethods = new HashMap<String, Method>(methods.size());
		for (Method method : methods) {
			if (method.isAnnotationPresent(iri.class)) {
				String uri = method.getAnnotation(iri.class).value();
				if (!namedMethods.containsKey(uri)
						|| !isBridge(method, methods)) {
					namedMethods.put(uri, method);
				}
			}
		}
		for (Method method : methods) {
			if (!method.getName().startsWith("_$")) {
				boolean bridge = isBridge(method, methods);
				implementMethod(method, method.getName(), bridge);
			}
		}
		return cp.createClass(cc);
	}

	private void addInterfaces(Class<?> clazz) {
		if (interfaces.contains(clazz))
			return;
		if (clazz.isInterface()) {
			interfaces.add(clazz);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			addInterfaces(superclass);
		}
		for (Class<?> face : clazz.getInterfaces()) {
			if (!isSpecial(face)) {
				addInterfaces(face);
			}
		}
	}

	private boolean isSpecial(Class<?> face) {
		return special.contains(face.getName());
	}

	private Collection<Method> getMethods() {
		Map map = new HashMap();
		for (Class<?> jc : javaClasses) {
			for (Method m : jc.getMethods()) {
				if (isSpecial(m))
					continue;
				List list = new ArrayList(getParameterTypes(m).length + 1);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(getParameterTypes(m)));
				if (!map.containsKey(list)) {
					map.put(list, m);
				}
			}
		}
		for (Class<?> jc : interfaces) {
			for (Method m : jc.getMethods()) {
				if (isSpecial(m))
					continue;
				List list = new ArrayList(getParameterTypes(m).length + 1);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(getParameterTypes(m)));
				if (!map.containsKey(list)) {
					map.put(list, m);
				}
			}
		}
		return map.values();
	}

	private boolean isSpecial(Method m) {
		if (isTransient(m.getModifiers()))
			return true;
		return Object.class.equals(m.getDeclaringClass());
	}

	private Class<?>[] getParameterTypes(Method m) {
		if (m.isAnnotationPresent(parameterTypes.class))
			return m.getAnnotation(parameterTypes.class).value();
		return m.getParameterTypes();
	}

	private boolean isBridge(Method method, Collection<Method> methods) {
		for (Method m : methods) {
			if (!m.getName().equals(method.getName()))
				continue;
			if (!Arrays.equals(getParameterTypes(m), getParameterTypes(method)))
				continue;
			if (m.getReturnType().equals(method.getReturnType()))
				continue;
			if (m.getReturnType().isAssignableFrom(method.getReturnType()))
				return true;
		}
		return false;
	}

	private boolean implementMethod(Method method, String name, boolean bridge)
			throws Exception {
		List<Class<?>> chain = chain(method);
		List<Object[]> implementations = getImpls(chain, method);
		if (implementations.isEmpty())
			return false;
		Class<?> type = method.getReturnType();
		boolean chained = implementations.size() > 1
				|| !type.equals(((Method) implementations.get(0)[1])
						.getReturnType()) || isMessage(chain, method);
		Method face = findInterfaceMethod(method);
		CodeBuilder body = cc.copyMethod(face, name, bridge);
		Class<?> superclass1 = cc.getSuperclass();
		Set<Field> fieldsRead = getFieldsRead(superclass1, method);
		Set<Field> fieldsWriten = getFieldsWritten(superclass1, method);
		if (!fieldsRead.isEmpty() || !fieldsWriten.isEmpty()) {
			if (!cc.getDeclaredFieldNames().contains("_$incall")) {
				cc.createField(Integer.TYPE, "_$incall");
			}
			body.declareObject(Boolean.TYPE, "subcall").code("_$incall > 0")
					.semi();
			body.assign("_$incall").code("_$incall + 1").semi();
			body.code("try {\n");
			body.code("if (!subcall) {\n");
			int count = 0;
			for (Field field : fieldsRead) {
				populateField(field, superclass1, body, count++);
			}
			body.code("}\n");
		}
		boolean voidReturnType = type.equals(Void.TYPE);
		boolean primitiveReturnType = type.isPrimitive();
		boolean setReturnType = type.equals(Set.class);
		String proceed = ".proceed();\n";
		if (chained) {
			if (!voidReturnType && primitiveReturnType) {
				proceed = ".getFunctionalLiteralResponse();\n";
				body.code(type.getName()).code(" result;\n");
			} else if (setReturnType) {
				proceed = ".getObjectResponse();\n";
				body.code(Set.class.getName() + " result;\n");
			} else if (!voidReturnType) {
				proceed = ".getFunctionalObjectResponse();\n";
				body.code(Object.class.getName() + " result;\n");
			}
		} else {
			body.code("return ($r) ");
		}
		boolean chainStarted = false;
		for (Object[] ar : implementations) {
			assert ar.length == 2;
			String target = (String) ar[0];
			Method m = (Method) ar[1];
			if (chained) {
				if ("super".equals(target)) {
					if (chainStarted) {
						body.code(proceed);
						conditionalReturn(type, body);
						chainStarted = false;
					}
					appendMethodCall(m, target, body);
				} else {
					if (!chainStarted) {
						chainStarted = true;
						if (!type.equals(Void.TYPE)) {
							body.code("result = ($r) ");
						}
						body.code("new ");
						body.code(InvocationMessageContext.class.getName());
						body.code("($0, ");
						Class<?> mtype = getMessageType(method);
						if (mtype != null) {
							body.insert(mtype);
							body.code(", ");
						}
						body.insert(face);
						body.code(", $args)\n");
					}
					appendInvocation(m, target, body);
				}
			} else {
				body.code(getMethodCall(m, target));
			}
		}
		if (chainStarted) {
			body.code(proceed);
			chainStarted = false;
		}
		if (chained) {
			if (!type.equals(Void.TYPE)) {
				body.code("return ($r) result;\n");
			}
		}
		if (!fieldsRead.isEmpty() || !fieldsWriten.isEmpty()) {
			body.code("} finally {\n");
			body.assign("_$incall").code("_$incall - 1").semi();
			body.code("if (!subcall) {\n");
			int count = 0;
			for (Field field : fieldsWriten) {
				saveFieldValue(field, superclass1, body, count++);
			}
			body.code("}\n");
			body.code("}\n");
		}
		body.end();
		return true;
	}

	private Class<?> getMessageType(Method method) {
		if (method.isAnnotationPresent(iri.class)) {
			String id = method.getAnnotation(iri.class).value();
			URIImpl uri = new URIImpl(id);
			return mapper.findInterfaceConcept(uri);
		}
		return null;
	}

	private List<Class<?>> chain(Method method) throws Exception {
		if (behaviours == null)
			return null;
		int size = behaviours.size();
		List<Class<?>> all = new ArrayList<Class<?>>(size);
		for (Class<?> behaviour : behaviours) {
			if (isMethodPresent(behaviour, method)) {
				all.add(behaviour);
			}
		}
		Iterator<Class<?>> iter;
		List<Class<?>> rest = new ArrayList<Class<?>>(all.size());
		// sort plain methods before @precedes methods
		iter = all.iterator();
		while (iter.hasNext()) {
			Class<?> behaviour = iter.next();
			if (!isOverridesPresent(behaviour)) {
				rest.add(behaviour);
				iter.remove();
			}
		}
		rest.addAll(all);
		all = rest;
		rest = new ArrayList<Class<?>>(all.size());
		// sort intercepting methods before plain methods
		iter = all.iterator();
		while (iter.hasNext()) {
			Class<?> behaviour = iter.next();
			if (isMessage(behaviour, method)) {
				rest.add(behaviour);
				iter.remove();
			}
		}
		rest.addAll(all);
		// sort by @precedes annotations
		List<Class<?>> list = new ArrayList<Class<?>>(rest.size());
		while (!rest.isEmpty()) {
			int before = rest.size();
			iter = rest.iterator();
			loop: while (iter.hasNext()) {
				Class<?> b1 = iter.next();
				for (Class<?> b2 : rest) {
					if (overrides(b2, b1)) {
						continue loop;
					}
				}
				list.add(b1);
				iter.remove();
			}
			if (before <= rest.size())
				throw new ObjectCompositionException("Invalid method chain: "
						+ rest.toString());
		}
		return list;
	}

	private boolean isMessage(List<Class<?>> behaviours, Method method)
			throws Exception {
		if (behaviours != null) {
			for (Class<?> behaviour : behaviours) {
				if (isMessage(behaviour, method))
					return true;
			}
		}
		return false;
	}

	/**
	 * @return list of <String, Method>
	 */
	private List<Object[]> getImpls(List<Class<?>> behaviours, Method method)
			throws Exception {
		List<Object[]> list = new ArrayList<Object[]>();
		Class<?> type = method.getReturnType();
		Class<?> superclass = cc.getSuperclass();
		Class<?>[] types = getParameterTypes(method);
		if (method.getName().equals("toString") && types.length == 0) {
			// toString give priority treatment to concept's implementation
			Method m = superclass.getMethod(method.getName(), types);
			if (!m.getDeclaringClass().equals(Object.class)) {
				list.add(new Object[] { "super", m });
			}
		}
		if (behaviours != null) {
			for (Class<?> behaviour : behaviours) {
				String target = getGetterName(behaviour) + "()";
				list.add(new Object[] { target, getMethod(behaviour, method) });
			}
		}
		if (!superclass.equals(Object.class)) {
			try {
				Method m = superclass.getMethod(method.getName(), types);
				Class<?> returnType = m.getReturnType();
				if (!isAbstract(m.getModifiers()) && returnType.equals(type)) {
					list.add(new Object[] { "super", m });
				}
			} catch (NoSuchMethodException e) {
				// no super method
			}
		}
		for (Method m : getSubMethods(method)) {
			if (m.equals(method))
				continue;
			list.addAll(getImpls(chain(m), m));
		}
		return list;
	}

	private List<Method> getSubMethods(Method method) {
		for (Annotation ann : method.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (RDFS.SUBCLASSOF.equals(mapper.findAnnotation(type))) {
				try {
					Object value = type.getMethod("value").invoke(ann);
					String[] uris = (String[]) value;
					List<Method> list = new ArrayList<Method>(uris.length);
					for (String uri : uris) {
						Method m = namedMethods.get(uri);
						if (m != null) {
							list.add(m);
						}
					}
					return list;
				} catch (Exception e) {
					continue;
				}
			}
		}
		return Collections.emptyList();
	}

	private void appendInvocation(Method method, String target, CodeBuilder body) {
		body.code(".appendInvocation(");
		body.code(target).code(", ");
		body.insert(method);
		body.code(")\n");
	}

	private String getMethodCall(Method method, String target) {
		StringBuilder eval = new StringBuilder();
		eval.append(target);
		eval.append(".").append(method.getName()).append("($$);\n");
		return eval.toString();
	}

	private Method findInterfaceMethod(Method method) {
		String name = method.getName();
		Class<?> type = method.getReturnType();
		Class<?>[] types = getParameterTypes(method);
		Class<?>[] faces = cc.getInterfaces();
		Method m = findInterfaceMethod(faces, name, type, types);
		if (m != null)
			return m;
		return method;
	}

	private Method findInterfaceMethod(Class<?>[] interfaces, String name,
			Class<?> type, Class<?>[] types) {
		for (Class face : interfaces) {
			try {
				Method m = face.getDeclaredMethod(name, types);
				if (m.getReturnType().equals(type))
					return m;
			} catch (NoSuchMethodException e) {
				// continue
			}
			Class[] faces = face.getInterfaces();
			Method m = findInterfaceMethod(faces, name, type, types);
			if (m != null)
				return m;
		}
		return null;
	}

	private void populateField(Field field, Class<?> superclass,
			CodeBuilder body, int count) throws Exception {
		int mod = field.getModifiers();
		if (!isPublic(mod) && !isProtected(mod)) {
			String fieldVar = field.getName() + "Field" + count;
			body.declareObject(Field.class, fieldVar);
			body.insert(field.getDeclaringClass());
			body.code(".getDeclaredField(\"");
			body.code(field.getName()).code("\")").semi();
			body.code(fieldVar).code(".setAccessible(true)").semi();
			body.code(fieldVar).code(".set");
			if (field.getType().isPrimitive()) {
				String tname = field.getType().getName();
				body.code(tname.substring(0, 1).toUpperCase());
				body.code(tname.substring(1));
			}
			body.code("(this, ");
		} else {
			body.code(field.getName()).code(" = ");
		}
		Method getter = propertyResolver.getReadMethod(field);
		for (Class<?> behaviour : behaviours) {
			if (getter.getDeclaringClass().isAssignableFrom(behaviour)) {
				body.code(getGetterName(behaviour)).code("().");
				break;
			}
		}
		body.code(getter.getName()).code("()");
		if (!isPublic(mod) && !isProtected(mod)) {
			body.code(")");
		}
		body.semi();
	}

	private void saveFieldValue(Field field, Class<?> superclass,
			CodeBuilder body, int count) throws Exception {
		String fieldVar = field.getName() + "Field" + count;
		body.declareObject(Field.class, fieldVar);
		body.insert(field.getDeclaringClass());
		body.code(".getDeclaredField(\"");
		body.code(field.getName()).code("\")").semi();
		body.code(fieldVar).code(".setAccessible(true)").semi();
		Method setter = propertyResolver.getWriteMethod(field);
		for (Class<?> behaviour : behaviours) {
			if (setter.getDeclaringClass().isAssignableFrom(behaviour)) {
				body.code(getGetterName(behaviour)).code("().");
				break;
			}
		}
		body.code(setter.getName()).code("(");
		int mod = field.getModifiers();
		if (!isPublic(mod) && !isProtected(mod)) {
			StringBuilder sb = new StringBuilder();
			sb.append(fieldVar).append(".get");
			if (field.getType().isPrimitive()) {
				String tname = field.getType().getName();
				sb.append(tname.substring(0, 1).toUpperCase());
				sb.append(tname.substring(1));
			}
			sb.append("(this)");
			if (field.getType().isPrimitive()) {
				body.code(sb.toString());
			} else {
				body.castObject(sb.toString(), field.getType());
			}
		} else {
			body.code(field.getName());
		}
		body.code(")").semi();
	}

	private void appendMethodCall(Method method, String target, CodeBuilder body) {
		Class<?> type = method.getReturnType();
		boolean voidReturnType = type.equals(Void.TYPE);
		if (!voidReturnType)
			body.code("result = ");
		body.code(getMethodCall(method, target));
		conditionalReturn(type, body);
	}

	private void conditionalReturn(Class<?> type, CodeBuilder body) {
		boolean voidReturnType = type.equals(Void.TYPE);
		boolean booleanReturnType = type.equals(Boolean.TYPE);
		boolean primitiveReturnType = type.isPrimitive();
		if (booleanReturnType) {
			body.code("if (result) return result;\n");
		} else if (!voidReturnType && primitiveReturnType) {
			body.code("if (result != 0) return ($r) result;\n");
		} else if (!voidReturnType) {
			body.code("if (result != null) return ($r) result;\n");
		}
	}

	private Set<Field> getFieldsRead(Class<?> superclass, Method method)
			throws Exception {
		if (superclass.equals(Object.class))
			return Collections.emptySet();
		ClassTemplate t = cp.loadClassTemplate(superclass);
		Set<Field> fields = t.getFieldsRead(method);
		Set<Field> accessed = new HashSet<Field>(fields.size());
		for (Field field : fields) {
			if (propertyResolver.getReadMethod(field) != null) {
				if (field.getDeclaringClass().isAssignableFrom(superclass)) {
					accessed.add(field);
				}
			}
		}
		return accessed;
	}

	private Set<Field> getFieldsWritten(Class<?> superclass, Method method)
			throws Exception {
		if (superclass.equals(Object.class))
			return Collections.emptySet();
		ClassTemplate t = cp.loadClassTemplate(superclass);
		Set<Field> fields = t.getFieldsWritten(method);
		Set<Field> accessed = new HashSet<Field>(fields.size());
		for (Field field : fields) {
			if (propertyResolver.getReadMethod(field) != null) {
				if (field.getDeclaringClass().isAssignableFrom(superclass)) {
					accessed.add(field);
				}
			}
		}
		return accessed;
	}

	private boolean isMethodPresent(Class<?> javaClass, Method method)
			throws Exception {
		return getMethod(javaClass, method) != null;
	}

	private boolean isMessage(Class<?> javaClass, Method method)
			throws Exception {
		return getMethod(javaClass, method).isAnnotationPresent(
				parameterTypes.class);
	}

	private Method getMethod(Class<?> javaClass, Method method)
			throws Exception {
		Class<?>[] types = getParameterTypes(method);
		try {
			Method m = javaClass.getMethod(method.getName(), types);
			if (!isTransient(m.getModifiers()) && !isObjectMethod(m))
				return m;
		} catch (NoSuchMethodException e) {
			// look at @parameterTypes
		}
		for (Method m : javaClass.getMethods()) {
			if (m.getName().equals(method.getName())) {
				parameterTypes ann = m.getAnnotation(parameterTypes.class);
				if (ann != null && Arrays.equals(ann.value(), types))
					return m;
			}
		}
		return null;
	}

	private boolean isOverridesPresent(Class<?> javaClass) {
		for (Annotation ann : javaClass.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (OBJ.PRECEDES.equals(mapper.findAnnotation(type)))
				return true;
		}
		return false;
	}

	private boolean overrides(Class<?> javaClass, Class<?> b1) throws Exception {
		for (Annotation ann : javaClass.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (OBJ.PRECEDES.equals(mapper.findAnnotation(type))) {
				Method m = type.getMethod("value");
				for (Class<?> c : ((Class<?>[]) m.invoke(ann))) {
					if (c.equals(b1))
						return true;
				}
			}
		}
		return false;
	}

	private String getGetterName(Class<?> javaClass) {
		return "_$get" + javaClass.getSimpleName()
				+ Integer.toHexString(javaClass.getName().hashCode());
	}

	private boolean addBehaviour(Class<?> javaClass) throws Exception {
		try {
			String getterName = getGetterName(javaClass);
			String fieldName = "_$" + getterName.substring(5);
			cc.createField(javaClass, fieldName);
			CodeBuilder code = cc.createPrivateMethod(javaClass, getterName);
			code.code("if (").code(fieldName).code(" != null){\n");
			code.code("return ").code(fieldName).code(";\n} else {\n");
			code.code("return ").code(fieldName).code(" = ($r) ");
			code.code("new ").code(javaClass.getName());
			try {
				javaClass.getConstructor(ManagedRDFObject.class);
				code.code("($0)");
			} catch (NoSuchMethodException e) {
				javaClass.getConstructor();
				code.code("()");
			}
			code.code(";\n}").end();
			return true;
		} catch (NoSuchMethodException e) {
			// no default constructor
			return false;
		}
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}
}
