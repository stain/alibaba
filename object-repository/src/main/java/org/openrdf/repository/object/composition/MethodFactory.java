package org.openrdf.repository.object.composition;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodFactory {
	private Method method;
	private Class<?> factoryClass;

	public MethodFactory(Method method, Class<?> factoryClass) {
		assert method.getDeclaringClass().isAssignableFrom(factoryClass) : factoryClass
				.getSimpleName()
				+ method;
		this.method = method;
		this.factoryClass = factoryClass;
	}

	public Method getMethod() {
		return method;
	}

	public Class<?> getFactoryClass() {
		return factoryClass;
	}

	public Class<?> getBehaviourClass() {
		return (Class<?>) method.getReturnType();
	}

	public Method getInstanceMethod() {
		try {
			Method getInstance = factoryClass.getDeclaredMethod("getInstance");
			if (!Modifier.isStatic(getInstance.getModifiers()))
				return null;
			Class<?> declaring = method.getDeclaringClass();
			if (declaring.isAssignableFrom(getInstance.getReturnType()))
				return getInstance;
		} catch (NoSuchMethodException e) {
		}
		return null;
	}

	public String toString() {
		return factoryClass.getSimpleName() + " " + method.getName();
	}

}
