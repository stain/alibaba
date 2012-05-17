/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.repository.object.composition.helpers;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPublic;

import java.lang.annotation.Annotation;
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

import org.openrdf.annotations.InstancePrivate;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.composition.ClassTemplate;
import org.openrdf.repository.object.composition.CodeBuilder;
import org.openrdf.repository.object.composition.MethodBuilder;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.RoleMapper;
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
	public static String getPrivateBehaviourMethod(BehaviourFactory factory) {
		String simpleName = factory.getName().replaceAll("\\W", "_");
		return "_$get" + simpleName + "Behaviour" + Integer.toHexString(System.identityHashCode(factory));
	}

	public static void calling(Object target, String method, Object[] args) {
		if (++count % 512 == 0){
			Throwable stack = new Throwable();
			StackTraceElement[] trace = stack.getStackTrace();
			if(trace.length > 512) {
				String string = Arrays.asList(args).toString();
				String substring = string.substring(1, string.length() - 1);
				logger.warn(target + "<-" + method + "(" + substring + ")", stack);
			}
		}
	}

	private static int count;
	private static Set<String> special = new HashSet<String>(Arrays.asList(
			"groovy.lang.GroovyObject", RDFObjectBehaviour.class.getName()));
	private static Logger logger = LoggerFactory.getLogger(ClassCompositor.class);
	private ClassFactory cp;
	private RoleMapper mapper;
	private final String className;
	private Class<?> baseClass = Object.class;
	private final Set<Class<?>> interfaces;
	private final Set<BehaviourFactory> allBehaviours;
	private Collection<Method> methods;
	private Map<String, Method> namedMethods;
	private Map<Method, String> superMethods = new HashMap<Method, String>();
	private Map<String, Set<BehaviourFactory>> behaviours;
	private ClassTemplate cc;

	public ClassCompositor(String className, int size) {
		this.className = className;
		interfaces = new LinkedHashSet<Class<?>>(size);
		allBehaviours = new LinkedHashSet<BehaviourFactory>(size);
	}

	public void setClassFactory(ClassFactory cp) {
		this.cp = cp;
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

	public void addAllBehaviours(
			Collection<? extends BehaviourFactory> factories) {
		this.allBehaviours.addAll(factories);
	}

	public Class<?> compose() throws Exception {
		logger.trace("public class {} extends {}", className, baseClass);
		cc = cp.createClassTemplate(className, baseClass);
		for (BehaviourFactory behaviours : allBehaviours) {
			for (Class<?> clazz : behaviours.getInterfaces()) {
				addInterfaces(clazz);
			}
		}
		for (Class<?> face : interfaces) {
			cc.addInterface(face);
		}
		behaviours = new HashMap<String, Set<BehaviourFactory>>();
		for (BehaviourFactory clazz : allBehaviours) {
			addBehaviour(clazz);
			for (Method m : clazz.getMethods()) {
				if (!isSpecial(m)) {
					Set<BehaviourFactory> s = behaviours.get(m.getName());
					if (s == null) {
						s = new HashSet<BehaviourFactory>();
						behaviours.put(m.getName(), s);
					}
					s.add(clazz);
				}
			}
		}
		methods = getMethods();
		namedMethods = new HashMap<String, Method>(methods.size());
		for (Method method : methods) {
			if (method.isAnnotationPresent(Iri.class)) {
				String uri = method.getAnnotation(Iri.class).value();
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
		try {
			Class<?> createdClass = cp.createClass(cc);
			for (BehaviourFactory clazz : allBehaviours) {
				populateBehaviourField(clazz, createdClass);
			}
			return createdClass;
		} catch (LinkageError e) {
			String msg = e.getMessage() + " while composing "
					+ baseClass.getSimpleName() + " with " + interfaces;
			LinkageError error = new LinkageError(msg);
			error.initCause(e);
			throw error;
		}
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
			if (isPublic(face.getModifiers()) && !isSpecial(face)) {
				addInterfaces(face);
			}
		}
	}

	private boolean isSpecial(Class<?> face) {
		return special.contains(face.getName());
	}

	private Collection<Method> getMethods() {
		Map<List<?>, Method> map = new HashMap<List<?>, Method>();
		for (Class<?> jc : interfaces) {
			for (Method m : jc.getMethods()) {
				if (isSpecial(m))
					continue;
				Class<?>[] ptypes = getParameterTypes(m);
				List list = new ArrayList(ptypes.length + 2);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(ptypes));
				if (map.containsKey(list)) {
					if (getRank(m) > getRank(map.get(list))) {
						map.put(list, m);
					}
				} else {
					map.put(list, m);
				}
			}
		}
		for (BehaviourFactory factory : allBehaviours) {
			for (Method m : factory.getMethods()) {
				if (isSpecial(m))
					continue;
				Class<?>[] ptypes = getParameterTypes(m);
				List list = new ArrayList(ptypes.length + 2);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(ptypes));
				if (map.containsKey(list)) {
					if (getRank(m) > getRank(map.get(list))) {
						map.put(list, m);
					}
				} else {
					map.put(list, m);
				}
			}
		}
		return map.values();
	}

	private int getRank(Method m) {
		int rank = m.getAnnotations().length;
		if (m.isAnnotationPresent(ParameterTypes.class))
			return rank - 2;
		return rank;
	}

	private boolean isSpecial(Method m) {
		if (isObjectMethod(m))
			return true;
		if ("methodMissing".equals(m.getName()))
			return true;
		if ("propertyMissing".equals(m.getName()))
			return true;
		return m.isAnnotationPresent(InstancePrivate.class);
	}

	private Class<?>[] getParameterTypes(Method m) {
		if (m.isAnnotationPresent(ParameterTypes.class))
			return m.getAnnotation(ParameterTypes.class).value();
		return m.getParameterTypes();
	}

	private boolean isBridge(Method method, Collection<Method> methods) {
		for (Method m : methods) {
			if (!m.getName().equals(method.getName()))
				continue;
			if (!Arrays.equals(getParameterTypes(m), getParameterTypes(method)))
				continue;
			if (!m.getReturnType().isAssignableFrom(method.getReturnType()))
				return true;
		}
		return false;
	}

	private boolean implementMethod(Method method, String name, boolean bridge)
			throws Exception {
		List<BehaviourMethod> chain = chain(method);
		List<Object[]> implementations = getImpls(chain, method);
		if (implementations.isEmpty())
			return false;
		Class<?> type = method.getReturnType();
		boolean voidReturnType = type.equals(Void.TYPE);
		boolean chained = implementations.size() > 1 && !voidReturnType
				|| isChainRequired(chain, method);
		Method face = findInterfaceMethod(method);
		CodeBuilder body = cc.copyMethod(face, name, bridge);
		boolean primitiveReturnType = type.isPrimitive();
		boolean setReturnType = type.equals(Set.class);
		if (logger.isTraceEnabled()) {
			body.code(ClassCompositor.class.getName() + ".calling(this, \""
					+ method.getName() + "\", $args);");
		}
		if (chained) {
			if (!voidReturnType && primitiveReturnType) {
				body.code(type.getName()).code(" result;\n");
			} else if (setReturnType) {
				body.code(Set.class.getName() + " result;\n");
			} else if (!voidReturnType) {
				body.code(Object.class.getName() + " result;\n");
			}
		} else if (!voidReturnType) {
			body.code("return ($r) ");
		}
		boolean chainStarted = false;
		for (Object[] ar : implementations) {
			assert ar.length == 2;
			String target = (String) ar[0];
			Method m = (Method) ar[1];
			if (chained) {
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
				if ("super".equals(target)) {
					String dname = createSuperCall(m);
					appendInvocation("this", dname, m.getParameterTypes(), body);
				} else {
					appendInvocation(target, m, body);
				}
			} else {
				body.code(getMethodCall(target, m));
			}
		}
		if (chainStarted) {
			body.code(".proceed();\n");
			chainStarted = false;
		}
		if (chained && !voidReturnType) {
			body.code("return ($r) result;\n");
		}
		body.end();
		return true;
	}

	private String createSuperCall(Method m) {
		if (superMethods.containsKey(m))
			return superMethods.get(m);
		Class<?> rtype = m.getReturnType();
		Class<?>[] ptypes = m.getParameterTypes();
		String name = "_$super" + superMethods.size() + "_" + m.getName();
		MethodBuilder delegating = cc.createMethod(rtype, name, ptypes);
		if (!Void.TYPE.equals(rtype))
			delegating.code("return ");
		delegating.code(getMethodCall("super", m)).end();
		superMethods.put(m, name);
		return name;
	}

	private Class<?> getMessageType(Method method) {
		if (!isProperty(method) && method.isAnnotationPresent(Iri.class)) {
			String id = method.getAnnotation(Iri.class).value();
			URIImpl uri = new URIImpl(id);
			return mapper.findInterfaceConcept(uri);
		}
		return null;
	}

	private boolean isProperty(Method method) {
		int argc = method.getParameterTypes().length;
		String name = method.getName();
		Class<?> rt = method.getReturnType();
		boolean returns = !Void.TYPE.equals(rt);
		if (argc == 0 && returns && name.startsWith("get"))
			return true;
		if (argc == 0 && Boolean.TYPE.equals(rt) && name.startsWith("is"))
			return true;
		if (argc == 1 && !returns && name.startsWith("set"))
			return true;
		return false;
	}

	private List<BehaviourMethod> chain(Method method) throws Exception {
		if (behaviours.isEmpty())
			return null;
		Set<BehaviourFactory> set = behaviours.get(method.getName());
		List<BehaviourMethod> list = new ArrayList<BehaviourMethod>();
		if (set != null) {
			for (BehaviourFactory behaviour : set) {
				Method m = behaviour.getInvocation(method);
				if (m != null) {
					list.add(new BehaviourMethod(behaviour, m));
				}
			}
		}
		for (Method m : getSuperMethods(method)) {
			if (m.equals(method))
				continue;
			list.addAll(chain(m));
		}
		return list;
	}

	private List<BehaviourMethod> sort(List<BehaviourMethod> post) {
		Iterator<BehaviourMethod> iter;
		List<BehaviourMethod> pre = new ArrayList<BehaviourMethod>(post.size());
		// sort @precedes methods before plain methods
		iter = post.iterator();
		while (iter.hasNext()) {
			BehaviourMethod behaviour = iter.next();
			if (behaviour.isOverridesPresent()) {
				pre.add(behaviour);
				iter.remove();
			}
		}
		pre.addAll(post);
		post = pre;
		pre = new ArrayList<BehaviourMethod>(post.size());
		// sort intercepting methods before plain methods
		iter = post.iterator();
		while (iter.hasNext()) {
			BehaviourMethod behaviour = iter.next();
			if (behaviour.isMessage()) {
				pre.add(behaviour);
				iter.remove();
			}
		}
		pre.addAll(post);
		post = pre;
		pre = new ArrayList<BehaviourMethod>(post.size());
		// sort empty @precedes methods first
		iter = post.iterator();
		while (iter.hasNext()) {
			BehaviourMethod behaviour = iter.next();
			if (behaviour.isEmptyOverridesPresent()) {
				pre.add(behaviour);
				iter.remove();
			}
		}
		pre.addAll(post);
		post = pre;
		pre = new ArrayList<BehaviourMethod>(post.size());
		// sort by @precedes annotations
		while (!post.isEmpty()) {
			int before = post.size();
			iter = post.iterator();
			loop: while (iter.hasNext()) {
				BehaviourMethod b1 = iter.next();
				for (BehaviourMethod b2 : post) {
					if (b2.overrides(b1)) {
						continue loop;
					}
				}
				pre.add(b1);
				iter.remove();
			}
			if (before <= post.size())
				throw new ObjectCompositionException("Invalid method chain: "
						+ post.toString());
		}
		pre.addAll(post);
		return pre;
	}

	private boolean isChainRequired(List<BehaviourMethod> behaviours, Method method)
			throws Exception {
		if (behaviours != null) {
			for (BehaviourMethod behaviour : behaviours) {
				if (behaviour.isMessage())
					return true;
				Class<?> rt = behaviour.getMethod().getReturnType();
				if (!method.getReturnType().equals(rt))
					return true;
			}
		}
		return false;
	}

	/**
	 * @return list of <String, Method>
	 */
	private List<Object[]> getImpls(List<BehaviourMethod> behaviours, Method method)
			throws Exception {
		List<Object[]> list = new ArrayList<Object[]>();
		Class<?> type = method.getReturnType();
		Class<?> superclass = cc.getSuperclass();
		Class<?>[] types = getParameterTypes(method);
		if (behaviours != null) {
			for (BehaviourMethod behaviour : sort(new ArrayList<BehaviourMethod>(behaviours))) {
				if (behaviour.getFactory().isSingleton()) {
					String target = getBehaviourFieldName(behaviour.getFactory());
					list.add(new Object[] { target, behaviour.getMethod() });
				} else {
					String target = getPrivateBehaviourMethod(behaviour.getFactory()) + "()";
					list.add(new Object[] { target, behaviour.getMethod() });
				}
			}
		}
		if (!superclass.equals(Object.class)) {
			try {
				Method m = superclass.getMethod(method.getName(), types);
				Class<?> returnType = m.getReturnType();
				if (!isSpecial(m) && !isAbstract(m.getModifiers())
						&& returnType.equals(type)) {
					list.add(new Object[] { "super", m });
				}
			} catch (NoSuchMethodException e) {
				// no super method
			}
		}
		return list;
	}

	private List<Method> getSuperMethods(Method method) {
		if (!method.isAnnotationPresent(Iri.class))
			return Collections.emptyList();
		String iri = method.getAnnotation(Iri.class).value();
		Class<?> concept = mapper.findConcept(new URIImpl(iri), cp);
		if (concept == null)
			return Collections.emptyList();
		Set<URI> set = new LinkedHashSet<URI>();
		List<Method> list = new ArrayList<Method>();
		for (URI uri : getSuperClasses(concept, set)) {
			Method m = namedMethods.get(uri.stringValue());
			if (m != null && !isSpecial(m)) {
				list.add(m);
			}
		}
		return list;
	}

	private Set<URI> getSuperClasses(Class<?> concept, Set<URI> set) {
		Class<?> sup = concept.getSuperclass();
		if (sup != null) {
			URI uri = mapper.findType(sup);
			if (set.add(uri)) {
				getSuperClasses(sup, set);
			}
		}
		for (Class<?> face : concept.getInterfaces()) {
			URI uri = mapper.findType(face);
			if (uri != null && set.add(uri)) {
				getSuperClasses(face, set);
			}
		}
		return set;
	}

	private void appendInvocation(String target, Method method, CodeBuilder body) {
		body.code(".appendInvocation(");
		body.code(target).code(", ");
		body.insert(method);
		body.code(")\n");
	}

	private void appendInvocation(String target, String name,
			Class<?>[] params, CodeBuilder body) {
		body.code(".appendInvocation(");
		body.code(target).code(", ");
		body.insertMethod(name, params);
		body.code(")\n");
	}

	private String getMethodCall(String target, Method method) {
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
		m = findSuperMethod(cc.getSuperclass(), name, type, types);
		if (m != null)
			return m;
		return method;
	}

	private Method findInterfaceMethod(Class<?>[] interfaces, String name,
			Class<?> type, Class<?>[] types) {
		Method best = null;
		for (Class face : interfaces) {
			if (best != null) {
				Annotation[][] anns = best.getParameterAnnotations();
				for (int i = 0; i < anns.length; i++) {
					for (int j = 0; j < anns[i].length; j++) {
						if (anns[i][j].annotationType().equals(Iri.class)) {
							return best; // parameter IRI present
						}
					}
				}
			}
			try {
				Method m = face.getDeclaredMethod(name, types);
				if (m.getReturnType().equals(type)) {
					best = m;
					continue;
				}
			} catch (NoSuchMethodException e) {
				// continue
			}
			Class[] faces = face.getInterfaces();
			Method m = findInterfaceMethod(faces, name, type, types);
			if (m != null) {
				best = m;
			}
		}
		return best;
	}

	private Method findSuperMethod(Class<?> base, String name, Class<?> type,
			Class<?>[] types) {
		if (base == null)
			return null;
		try {
			Method m = base.getDeclaredMethod(name, types);
			if (m.getReturnType().equals(type))
				return m;
		} catch (NoSuchMethodException e) {
			// continue
		}
		Method m = findSuperMethod(base.getSuperclass(), name, type, types);
		if (m == null)
			return null;
		return m;
	}

	private void addBehaviour(BehaviourFactory factory) throws Exception {
		if (factory.isSingleton()) {
			String fieldName = getBehaviourFieldName(factory);
			Class<?> clazz = factory.getBehaviourType();
			cc.assignStaticField(clazz, fieldName).code("null").end();
		} else {
			String getterName = getPrivateBehaviourMethod(factory);
			String fieldFactoryName = getBehaviourFactoryFieldName(factory);
			cc.assignStaticField(BehaviourFactory.class, fieldFactoryName).code("null").end();
			String fieldName = getBehaviourFieldName(factory);
			Class<?> clazz = factory.getBehaviourType();
			cc.createField(clazz, fieldName);
			CodeBuilder code = cc.createPrivateMethod(clazz, getterName);
			code.code("if (").code(fieldName).code(" != null){\n");
			code.code("return ").code(fieldName).code(";\n} else {\n");
			code.code("return ").code(fieldName).code(" = ($r) ");
			code.code(fieldFactoryName).code(".newInstance($0)");
			code.code(";\n}").end();
		}
	}

	private void populateBehaviourField(BehaviourFactory factory,
			Class<?> createdClass) throws NoSuchFieldException,
			IllegalAccessException {
		if (factory.isSingleton()) {
			String fieldName = getBehaviourFieldName(factory);
			createdClass.getField(fieldName).set(null, factory.getSingleton());
		} else {
			String fieldName = getBehaviourFactoryFieldName(factory);
			createdClass.getField(fieldName).set(null, factory);
		}
	}

	private String getBehaviourFieldName(BehaviourFactory factory) {
		String getterName = getPrivateBehaviourMethod(factory);
		return "_$" + getterName.substring(5);
	}

	private String getBehaviourFactoryFieldName(BehaviourFactory factory) {
		String getterName = getPrivateBehaviourMethod(factory);
		return "_$" + getterName.substring(5) + "Factory";
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}
}
