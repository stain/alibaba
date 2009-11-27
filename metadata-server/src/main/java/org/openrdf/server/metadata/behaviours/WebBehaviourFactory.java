package org.openrdf.server.metadata.behaviours;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.precedes;
import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.ClassTemplate;
import org.openrdf.repository.object.composition.CodeBuilder;
import org.openrdf.repository.object.composition.MethodBuilder;
import org.openrdf.repository.object.concepts.Message;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.server.metadata.annotations.method;
import org.openrdf.server.metadata.annotations.operation;
import org.openrdf.server.metadata.concepts.InternalWebObject;

public class WebBehaviourFactory extends BehaviourFactory {

	private static final Method INIT_LOCAL;
	static {
		try {
			INIT_LOCAL = InternalWebObject.class.getMethod("initFileObject",
					File.class, Boolean.TYPE);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected boolean isEnhanceable(Class<?> role)
			throws ObjectStoreConfigException {
		for (Method method : role.getDeclaredMethods()) {
			if (method.isAnnotationPresent(operation.class))
				return true;
			if (method.isAnnotationPresent(method.class))
				return true;
		}
		return false;
	}

	@Override
	protected Collection<? extends Class<?>> findImplementations(Class<?> role)
			throws Exception {
		List<Class<?>> behaviours = new ArrayList<Class<?>>();
		for (Method method : role.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(operation.class)
					&& !method.isAnnotationPresent(method.class))
				continue;
			behaviours.add(findBehaviour(role, method));
		}
		return behaviours;
	}

	private Class<?> findBehaviour(Class<?> concept, Method method)
			throws Exception {
		String className = getJavaClassName(concept, method);
		try {
			return Class.forName(className, true, cp);
		} catch (ClassNotFoundException e1) {
			synchronized (cp) {
				try {
					return Class.forName(className, true, cp);
				} catch (ClassNotFoundException e2) {
					return implement(className, concept, method);
				}
			}
		}
	}

	private String getJavaClassName(Class<?> concept, Method method) {
		String suffix = getClass().getSimpleName().replaceAll("Factory$", "");
		String m = "$" + method.getName() + Math.abs(method.hashCode());
		return CLASS_PREFIX + concept.getName() + m + suffix;
	}

	private Class<?> implement(String className, Class<?> role, Method method)
			throws Exception {
		ClassTemplate cc = createBehaviourTemplate(className, role);
		if (!role.isInterface()) {
			cc.addAnnotation(precedes.class, role);
		}
		initFileObject(cc);
		overrideMethod(cc, method);
		return cp.createClass(cc);
	}

	private void initFileObject(ClassTemplate cc) {
		cc.createField(Boolean.TYPE, "local");
		CodeBuilder code = cc.overrideMethod(INIT_LOCAL, INIT_LOCAL.isBridge());
		code.code("local = true").semi().end();
	}

	private void overrideMethod(ClassTemplate cc, Method method) {
		Class<?> rt = method.getReturnType();
		MethodBuilder code;
		boolean intercepting = method.isAnnotationPresent(parameterTypes.class);
		Class<?>[] ptypes = method.getParameterTypes();
		intercepting &= ptypes.length == 1
				&& Message.class.isAssignableFrom(ptypes[0]);
		if (intercepting) {
			code = cc.createMethod(rt, method.getName(), ptypes[0]);
			ptypes = method.getAnnotation(parameterTypes.class).value();
			code.ann(parameterTypes.class, ptypes);
		} else {
			code = cc.createMethod(rt, method.getName(), Message.class);
			code.ann(parameterTypes.class, ptypes);
		}
		if (!Void.TYPE.equals(rt)) {
			code.code(Object.class.getName()).code(" result").semi();
		}
		code.code("if (local) {");
		code.code("$1.proceed()").semi();
		if (Set.class.equals(rt)) {
			code.code("result = $1.getObjectResponse()").semi();
		} else if (!Void.TYPE.equals(rt)) {
			code.code("result = $1.getFunctionalObjectResponse()").semi();
		}
		code.code("} else {");
		if (!Void.TYPE.equals(rt)) {
			code.code("result = ");
		}
		code.code("(").castObject(BEAN_FIELD_NAME, InternalWebObject.class);
		code.code(").invokeRemote(").insert(method);
		code.code(", $1.getParameters())").semi();
		code.code("}");
		if (rt.isPrimitive() && !Void.TYPE.equals(rt)) {
			if (Boolean.TYPE.equals(rt)) {
				code.code("if (result == null) return false;\n");
			} else {
				code.code("if (result == null) return ").cast("0", rt).semi();
			}
			code.declareWrapper(rt, "wrap").castObject("result", rt).semi();
			code.code("return wrap.").code(rt.getName()).code("Value()").semi();
		} else if (!Void.TYPE.equals(rt)) {
			code.code("return ").castObject("result", rt).semi();
		}
		code.end();
	}
}
