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

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isTransient;

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
import java.util.TreeSet;

import org.openrdf.repository.object.composition.helpers.BehaviourClass;
import org.openrdf.repository.object.composition.helpers.InvocationMessageContext;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes a collection of roles (interfaces or classes) and uses
 * composition to combine this into a single class.
 * 
 * @author James Leigh
 * 
 */
public class ClassCompositor {
	private static final String PKG_PREFIX = "object.proxies._";
	private static final String CLASS_PREFIX = "_EntityProxy";
	private Logger logger = LoggerFactory.getLogger(ClassCompositor.class);
	private Set<String> special = new HashSet<String>(Arrays.asList(
			"groovy.lang.GroovyObject", RDFObjectBehaviour.class.getName()));
	private PropertyMapperFactory propertyResolver;
	private AbstractClassFactory abstractResolver;
	private ClassFactory cp;
	private Collection<Class<?>> baseClassRoles;

	public void setInterfaceBehaviourResolver(PropertyMapperFactory loader) {
		this.propertyResolver = loader;
	}

	public void setAbstractBehaviourResolver(AbstractClassFactory loader) {
		this.abstractResolver = loader;
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

	public Class<?> resolveRoles(Collection<Class<?>> roles) {
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
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>(types.size());
		Set<Class<?>> abstracts = new LinkedHashSet<Class<?>>(types.size());
		Set<Class<?>> concretes = new LinkedHashSet<Class<?>>(types.size());
		Set<Class<?>> behaviours = new LinkedHashSet<Class<?>>(types.size());
		for (Class<?> role : types) {
			if (role.isInterface()) {
				interfaces.add(role);
			} else if (isAbstract(role.getModifiers())
					&& !baseClassRoles.contains(role)) {
				abstracts.add(role);
			} else {
				concretes.add(role);
			}
		}
		behaviours.addAll(concretes);
		behaviours.addAll(abstractResolver.findImplementations(abstracts));
		behaviours.addAll(propertyResolver.findImplementations(concretes));
		behaviours.addAll(propertyResolver.findImplementations(abstracts));
		behaviours.addAll(propertyResolver.findImplementations(interfaces));
		behaviours.removeAll(baseClassRoles);
		Class<?> baseClass = Object.class;
		types.retainAll(baseClassRoles);
		if (types.size() == 1) {
			baseClass = types.get(0);
		} else if (!types.isEmpty()) {
			logger.warn("Cannot compose multiple concept classes: " + types);
		}
		return composeBehaviours(className, baseClass, interfaces, behaviours);
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

	private Class<?> composeBehaviours(String className, Class<?> baseClass,
			Set<Class<?>> interfaces, Set<Class<?>> javaClasses)
			throws Exception {
		List<BehaviourClass> behaviours = new ArrayList<BehaviourClass>();
		ClassTemplate cc = cp.createClassTemplate(className, baseClass);
		for (Class<?> clazz : javaClasses) {
			addInterfaces(clazz, interfaces);
		}
		for (Class<?> face : interfaces) {
			cc.addInterface(face);
		}
		for (Class<?> clazz : javaClasses) {
			BehaviourClass behaviour = new BehaviourClass();
			behaviour.setJavaClass(clazz);
			behaviour.setDeclaring(cc);
			if (behaviour.init()) {
				behaviours.add(behaviour);
			}
		}
		if (baseClass != null) {
			javaClasses.add(baseClass);
		}
		Collection<Method> methods = getMethods(javaClasses);
		for (Method method : methods) {
			if (!method.getName().startsWith("_$")) {
				boolean bridge = isBridge(method, methods);
				implementMethod(behaviours, method, method.getName(), bridge, cc);
			}
		}
		return cp.createClass(cc);
	}

	private Collection<Method> getMethods(Set<Class<?>> javaClasses) {
		Map map = new HashMap();
		for (Class<?> jc : javaClasses) {
			for (Method m : jc.getMethods()) {
				if (isSpecial(m))
					continue;
				List list = new ArrayList(m.getParameterTypes().length + 1);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(m.getParameterTypes()));
				if (!map.containsKey(list)) {
					map.put(list, m);
				}
			}
		}
		return map.values();
	}

	private boolean isBridge(Method method, Collection<Method> methods) {
		for (Method m : methods) {
			if (!m.getName().equals(method.getName()))
				continue;
			if (!Arrays.equals(m.getParameterTypes(), method
					.getParameterTypes()))
				continue;
			if (m.getReturnType().equals(method.getReturnType()))
				continue;
			if (m.getReturnType().isAssignableFrom(method.getReturnType()))
				return true;
		}
		return false;
	}

	private Set<Class<?>> addInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
		if (interfaces.contains(clazz))
			return interfaces;
		if (clazz.isInterface()) {
			interfaces.add(clazz);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			addInterfaces(superclass, interfaces);
		}
		for (Class<?> face : clazz.getInterfaces()) {
			if (!isSpecial(face)) {
				addInterfaces(face, interfaces);
			}
		}
		return interfaces;
	}

	private boolean isSpecial(Method m) {
		if (isTransient(m.getModifiers()))
			return true;
		return Object.class.equals(m.getDeclaringClass());
	}

	private boolean isSpecial(Class<?> face) {
		return special.contains(face.getName());
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

	private boolean implementMethod(List<BehaviourClass> behaviours,
			Method method, String name, boolean bridge, ClassTemplate cc)
			throws Exception {
		List<BehaviourClass> chain = chain(behaviours, method);
		List<Object[]> implementations = getImpls(chain, method, cc);
		if (implementations.isEmpty())
			return false;
		boolean chained = implementations.size() > 1
				|| isMessage(chain, method);
		Class<?> type = method.getReturnType();
		CodeBuilder body = cc.copyMethod(method, name, bridge);
		Class<?> superclass1 = cc.getSuperclass();
		Set<Field> fieldsRead = getFieldsRead(superclass1, method, cc);
		Set<Field> fieldsWriten = getFieldsWritten(superclass1, method, cc);
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
				populateField(field, superclass1, behaviours, body, count++);
			}
			body.code("}\n");
		}
		boolean voidReturnType = type.equals(Void.TYPE);
		boolean primitiveReturnType = type.isPrimitive();
		if (chained) {
			if (!voidReturnType && primitiveReturnType) {
				body.code(type.getName()).code(" result;\n");
			} else if (!voidReturnType) {
				body.code("Object result;\n");
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
						body.code(".proceed();\n");
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
						body.insert(method);
						body.code(", $args)\n");
					}
					appendInvocation(m, target, body);
				}
			} else {
				body.code(getMethodCall(m, target));
			}
		}
		if (chainStarted) {
			body.code(".proceed();\n");
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
				saveFieldValue(field, superclass1, behaviours, body, count++);
			}
			body.code("}\n");
			body.code("}\n");
		}
		body.end();
		return true;
	}

	private List<BehaviourClass> chain(List<BehaviourClass> behaviours,
			Method method) throws Exception {
		if (behaviours == null)
			return null;
		int size = behaviours.size();
		List<BehaviourClass> all = new ArrayList<BehaviourClass>(size);
		for (BehaviourClass behaviour : behaviours) {
			if (behaviour.isMethodPresent(method)) {
				all.add(behaviour);
			}
		}
		// sort intercepting methods before plain methods
		List<BehaviourClass> rest = new ArrayList<BehaviourClass>(all.size());
		Iterator<BehaviourClass> iter = all.iterator();
		while (iter.hasNext()) {
			BehaviourClass behaviour = iter.next();
			if (behaviour.isMessage(method)) {
				rest.add(behaviour);
				iter.remove();
			}
		}
		rest.addAll(all);
		// sort by @subMethodOf annotations
		List<BehaviourClass> list = new ArrayList<BehaviourClass>(rest.size());
		while (!rest.isEmpty()) {
			int before = rest.size();
			iter = rest.iterator();
			loop: while (iter.hasNext()) {
				BehaviourClass b1 = iter.next();
				for (BehaviourClass b2 : rest) {
					if (b2.isSubMethodOf(b1, method)) {
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

	private boolean isMessage(List<BehaviourClass> behaviours, Method method)
			throws Exception {
		if (behaviours != null) {
			for (BehaviourClass behaviour : behaviours) {
				if (behaviour.isMessage(method))
					return true;
			}
		}
		return false;
	}

	/**
	 * @return list of <String, Method>
	 */
	private List<Object[]> getImpls(List<BehaviourClass> behaviours,
			Method method, ClassTemplate cc) throws Exception {
		List<Object[]> list = new ArrayList<Object[]>();
		Class<?> type = method.getReturnType();
		Class<?> superclass = cc.getSuperclass();
		Class<?>[] types = method.getParameterTypes();
		if (method.getName().equals("toString") && types.length == 0) {
			// toString give priority treatment to concept's implementation
			Method m = superclass.getMethod(method.getName(), types);
			if (!m.getDeclaringClass().equals(Object.class)) {
				list.add(new Object[] { "super", m });
			}
		}
		if (behaviours != null) {
			for (BehaviourClass behaviour : behaviours) {
				String target = behaviour.getGetterName() + "()";
				list.add(new Object[] { target, behaviour.getMethod(method) });
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
		return list;
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

	private void populateField(Field field, Class<?> superclass,
			List<BehaviourClass> behaviours, CodeBuilder body, int count)
			throws Exception {
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
		for (BehaviourClass behaviour : behaviours) {
			if (getter.getDeclaringClass().isAssignableFrom(
					behaviour.getJavaClass())) {
				body.code(behaviour.getGetterName()).code("().");
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
			List<BehaviourClass> behaviours, CodeBuilder body, int count)
			throws Exception {
		String fieldVar = field.getName() + "Field" + count;
		body.declareObject(Field.class, fieldVar);
		body.insert(field.getDeclaringClass());
		body.code(".getDeclaredField(\"");
		body.code(field.getName()).code("\")").semi();
		body.code(fieldVar).code(".setAccessible(true)").semi();
		Method setter = propertyResolver.getWriteMethod(field);
		for (BehaviourClass behaviour : behaviours) {
			if (setter.getDeclaringClass().isAssignableFrom(
					behaviour.getJavaClass())) {
				body.code(behaviour.getGetterName()).code("().");
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

	private Set<Field> getFieldsRead(Class<?> superclass, Method method, ClassTemplate t)
			throws Exception {
		if (superclass.equals(Object.class))
			return Collections.emptySet();
		ClassTemplate cc = t.loadClassTemplate(superclass);
		Set<Field> fields = cc.getFieldsRead(method);
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

	private Set<Field> getFieldsWritten(Class<?> superclass, Method method,
			ClassTemplate t) throws Exception {
		if (superclass.equals(Object.class))
			return Collections.emptySet();
		ClassTemplate cc = t.loadClassTemplate(superclass);
		Set<Field> fields = cc.getFieldsWritten(method);
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
}
