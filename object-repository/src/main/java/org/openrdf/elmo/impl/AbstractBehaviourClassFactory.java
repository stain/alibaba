package org.openrdf.elmo.impl;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isProtected;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.elmo.EntitySupport;
import org.openrdf.elmo.ImplementationResolver;
import org.openrdf.elmo.dynacode.ClassFactory;
import org.openrdf.elmo.dynacode.ClassTemplate;
import org.openrdf.elmo.dynacode.CodeBuilder;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ElmoCompositionException;

public class AbstractBehaviourClassFactory implements ImplementationResolver {
	private static final String GET_ENTITY_METHOD = "getSupportedElmoEntity";
	public static final String CLASS_PREFIX = "elmobeans.behaviours.";
	private static final String BEAN_FIELD_NAME = "_$elmoBean";
	private ClassFactory cp;

	public void setClassDefiner(ClassFactory definer) {
		this.cp = definer;
	}

	public Collection<Class<?>> findImplementations(Collection<Class<?>> classes) {
		try {
			List<Class<?>> result = new ArrayList<Class<?>>();
			for (Class<?> c : classes) {
				result.add(findClass(c));
			}
			return result;
		} catch (ElmoCompositionException e) {
			throw e;
		} catch (Exception e) {
			throw new ElmoCompositionException(e);
		}
	}

	public Method getReadMethod(Field field) {
		return null;
	}

	public Method getWriteMethod(Field field) {
		return null;
	}

	private Class<?> findClass(Class<?> c) throws Exception {
		String name = getClassName(c);
		try {
			return Class.forName(name, true, cp);
		} catch (ClassNotFoundException e1) {
			synchronized (cp) {
				try {
					return Class.forName(name, true, cp);
				} catch (ClassNotFoundException e2) {
					return createClass(name, c);
				}
			}
		}
	
	}

	private Class<?> createClass(String name, Class<?> c) throws Exception {
		ClassTemplate cc = cp.createClassTemplate(name, c);
		cc.addInterface(EntitySupport.class);
		cc.createField(RDFObject.class, BEAN_FIELD_NAME);
		addConstructor(c, cc);
		addEntitySupportMethod(cc);
		for (Method m : getMethods(c)) {
			if (isFinal(m.getModifiers()))
				continue;
			if (!isAbstract(m.getModifiers()))
				continue;
			Class<?> r = m.getReturnType();
			Class<?>[] types = m.getParameterTypes();
			CodeBuilder code = cc.createTransientMethod(r, m.getName(), types);
			if (!Void.TYPE.equals(r)) {
				code.code("return ($r) ");
			}
			if (m.getDeclaringClass().isInterface()) {
				code.code("(").castObject(BEAN_FIELD_NAME, m.getDeclaringClass());
				code.code(").").code(m.getName()).code("($$);").end();
			} else {
				code.code(BEAN_FIELD_NAME).code(".getClass().getMethod(");
				code.insert(m.getName()).code(", ").insert(types).code(")").code(".invoke(");
				code.code(BEAN_FIELD_NAME).code(", $args);").end();
			}
		}
		return cp.createClass(cc);
	}

	private void addEntitySupportMethod(ClassTemplate cc) {
		CodeBuilder method = cc.createMethod(RDFObject.class, GET_ENTITY_METHOD);
		method.code("return ").code(BEAN_FIELD_NAME).code(";").end();
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

	private void addConstructor(Class<?> c, ClassTemplate cc) throws Exception {
		String type = getConstructorParameterType(c);
		StringBuilder body = new StringBuilder();
		if (type != null) {
			body.append("super((");
			body.append(type).append(")");
			body.append("$1);");
		}
		body.append(BEAN_FIELD_NAME).append(" = $1;");
		cc.addConstructor(new Class<?>[] { RDFObject.class }, body.toString());
	}

	private String getClassName(Class<?> klass) {
		return CLASS_PREFIX + klass.getName() + "Behaviour";
	}

	private String getConstructorParameterType(Class<?> javaClass) throws Exception {
		for (Constructor<?> c : javaClass.getConstructors()) {
			Class<?>[] param = c.getParameterTypes();
			if (param.length == 1 && param[0].isInterface()) {
				return param[0].getName();
			} else if (param.length == 1 && param[0].equals(Object.class)) {
				return Object.class.getName();
			}
		}
		return null;
	}

}
