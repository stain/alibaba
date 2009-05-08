package org.openrdf.repository.object.composition;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isProtected;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;

public class AbstractClassFactory {
	public static final String CLASS_PREFIX = "object.behaviours.";
	private static final String BEAN_FIELD_NAME = "_$bean";
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
		} catch (ObjectCompositionException e) {
			throw e;
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
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
		cc.addInterface(RDFObjectBehaviour.class);
		if (!RDFObject.class.isAssignableFrom(c)) {
			cc.addInterface(RDFObject.class);
		}
		cc.createField(ManagedRDFObject.class, BEAN_FIELD_NAME);
		addConstructor(c, cc);
		addRDFObjectBehaviourMethod(cc);
		if (!RDFObject.class.isAssignableFrom(c)) {
			addRDFObjectMethod(cc);
		}
		for (Method m : getMethods(c)) {
			if (isFinal(m.getModifiers()))
				continue;
			if (!isAbstract(m.getModifiers()))
				continue;
			Class<?> r = m.getReturnType();
			Class<?>[] types = m.getParameterTypes();
			CodeBuilder code = cc.createTransientMethod(m);
			if (!Void.TYPE.equals(r)) {
				code.code("return ($r) ");
			}
			if (m.getDeclaringClass().isInterface()) {
				code.code("(").castObject(BEAN_FIELD_NAME,
						m.getDeclaringClass());
				code.code(").").code(m.getName()).code("($$);").end();
			} else {
				code.code(BEAN_FIELD_NAME).code(".getClass().getMethod(");
				code.insert(m.getName()).code(", ").insert(types).code(")")
						.code(".invoke(");
				code.code(BEAN_FIELD_NAME).code(", $args);").end();
			}
		}
		return cp.createClass(cc);
	}

	private void addRDFObjectBehaviourMethod(ClassTemplate cc) {
		cc.createMethod(ManagedRDFObject.class,
				RDFObjectBehaviour.GET_ENTITY_METHOD).code("return ").code(
				BEAN_FIELD_NAME).code(";").end();
	}

	private void addRDFObjectMethod(ClassTemplate cc) {
		cc.createMethod(ObjectConnection.class, RDFObject.GET_CONNECTION).code(
				"return ").code(BEAN_FIELD_NAME).code(".").code(
				RDFObject.GET_CONNECTION).code("();").end();
		cc.createMethod(Resource.class, RDFObject.GET_RESOURCE).code("return ")
				.code(BEAN_FIELD_NAME).code(".").code(RDFObject.GET_RESOURCE)
				.code("();").end();
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
		try {
			c.getConstructor(); // must have a default constructor
		} catch (NoSuchMethodException e) {
			throw new ObjectCompositionException(c.getSimpleName()
					+ " must have a default constructor");
		}
		StringBuilder body = new StringBuilder();
		body.append(BEAN_FIELD_NAME).append(" = $1;");
		cc.addConstructor(new Class<?>[] { ManagedRDFObject.class }, body.toString());
	}

	private String getClassName(Class<?> klass) {
		return CLASS_PREFIX + klass.getName() + "Behaviour";
	}

}
