package org.openrdf.repository.object.composition.helpers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.precedes;

public class BehaviourMethod {
	private final Class<?> javaClass;
	private final Method method;

	public BehaviourMethod(Class<?> behaviour, Method method) {
		assert behaviour != null;
		assert method != null;
		this.javaClass = behaviour;
		this.method = method;
	}

	private BehaviourMethod(Class<?> behaviour) {
		this.javaClass = behaviour;
		this.method = null;
	}

	public Class<?> getBehaviour() {
		return javaClass;
	}

	public Method getMethod() {
		return method;
	}

	public boolean isMessage() {
		return method.isAnnotationPresent(parameterTypes.class);
	}

	public boolean isEmptyOverridesPresent() {
		precedes ann = javaClass.getAnnotation(precedes.class);
		if (ann == null)
			return false;
		Class<?>[] values = ann.value();
		return values != null && values.length == 0;
	}

	public boolean isOverridesPresent() {
		return javaClass.isAnnotationPresent(precedes.class);
	}

	public boolean overrides(BehaviourMethod b1,
			boolean explicit, Collection<Class<?>> exclude) {
		if (b1.getBehaviour().equals(javaClass))
			return false;
		if (exclude.contains(javaClass))
			return false;
		exclude.add(javaClass);
		precedes ann = javaClass.getAnnotation(precedes.class);
		if (ann == null)
			return false;
		Class<?>[] values = ann.value();
		for (Class<?> c : values) {
			if (c.equals(b1.getBehaviour()))
				return true;
			BehaviourMethod cbm = new BehaviourMethod(c);
			if (c.isAssignableFrom(b1.getBehaviour()))
				return explicit || !b1.overrides(cbm, true, new HashSet<Class<?>>());
			if (cbm.overrides(b1, explicit, exclude))
				return explicit || !b1.overrides(cbm, true, new HashSet<Class<?>>());
		}
		return false;
	}
}
