package org.openrdf.repository.object.advice.behaviour;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.openrdf.annotations.ParameterTypes;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.composition.BehaviourFactory;

public class AdviceBehaviourFactory implements BehaviourFactory {
	private static final Method intercept = Advice.class.getMethods()[0];
	private final Advice advice;
	private final Method intercepting;
	private final Class<?>[] parameterTypes;
	private final Class<?> annotationType;

	public AdviceBehaviourFactory(Advice advice, Method intercepting,
			Class<?> annotationType) {
		assert advice != null;
		assert intercepting != null;
		assert annotationType != null;
		this.advice = advice;
		this.intercepting = intercepting;
		this.annotationType = annotationType;
		if (intercepting.isAnnotationPresent(ParameterTypes.class)) {
			parameterTypes = intercepting.getAnnotation(ParameterTypes.class)
					.value();
		} else {
			parameterTypes = intercepting.getParameterTypes();
		}
	}

	public String toString() {
		return getName();
	}

	public String getName() {
		return getBehaviourType().getSimpleName();
	}

	public Class<?> getBehaviourType() {
		return Advice.class;
	}

	public Class<?>[] getInterfaces() {
		return new Class<?>[0];
	}

	public Method[] getMethods() {
		return new Method[] { intercepting };
	}

	public synchronized Method getInvocation(Method method) {
		if (intercepting.equals(method))
			return intercept;
		if (intercepting.getName().equals(method.getName())) {
			Class<?>[] ptypes = method.getParameterTypes();
			if (Arrays.equals(ptypes, parameterTypes))
				return intercept;
			if (method.isAnnotationPresent(ParameterTypes.class)) {
				Class<?>[] aptypes = method.getAnnotation(ParameterTypes.class)
						.value();
				if (Arrays.equals(aptypes, parameterTypes))
					return intercept;
			}
		}
		return null;
	}

	public boolean precedes(Method invocation, BehaviourFactory factory,
			Method to) {
		if (factory instanceof AdviceBehaviourFactory) {
			AdviceBehaviourFactory o = (AdviceBehaviourFactory) factory;
			Class<?> cls = intercepting.getDeclaringClass();
			Class<?> ocls = o.intercepting.getDeclaringClass();
			if (cls.isAssignableFrom(ocls))
				return false;
			if (ocls.isAssignableFrom(cls))
				return true;
			if (intercepting.equals(o.intercepting)) {
				for (Annotation ann : intercepting.getAnnotations()) {
					if (ann.annotationType().equals(o.annotationType))
						return false;
					if (ann.annotationType().equals(annotationType))
						return true;
				}
			}
			return false;
		}
		return !to.isAnnotationPresent(ParameterTypes.class);
	}

	public boolean isSingleton() {
		return true;
	}

	public Object getSingleton() {
		return advice;
	}

	public Object newInstance(Object proxy) throws Throwable {
		return advice;
	}

}
