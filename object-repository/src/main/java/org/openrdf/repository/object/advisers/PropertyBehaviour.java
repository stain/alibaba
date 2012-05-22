package org.openrdf.repository.object.advisers;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advisers.helpers.PropertySet;
import org.openrdf.repository.object.traits.Mergeable;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.openrdf.repository.object.traits.PropertyConsumer;
import org.openrdf.repository.object.traits.Refreshable;

public final class PropertyBehaviour implements Advice, Mergeable,
		Refreshable, PropertyConsumer {
	private final Class<?> concept;
	private final Class<?> type;
	private final PropertyDescriptor pd;
	private final PropertySet property;

	public PropertyBehaviour(PropertySet property, PropertyDescriptor pd) {
		assert pd != null;
		assert property != null;
		this.concept = pd.getReadMethod().getDeclaringClass();
		this.type = pd.getReadMethod().getReturnType();
		this.pd = pd;
		this.property = property;
	}

	public void usePropertyBindings(String binding, List<BindingSet> results) {
		if (property instanceof PropertyConsumer) {
			String var = binding + "_" + pd.getName();
			if (results.get(0).hasBinding(var)) {
				PropertyConsumer pc = (PropertyConsumer) property;
				pc.usePropertyBindings(var, results);
			}
		}
	}

	public void refresh() {
		property.refresh();
	}

	public void merge(Object source) throws RepositoryException {
		if (concept.isAssignableFrom(source.getClass())) {
			try {
				Object value = pd.getReadMethod().invoke(source);
				if (value != null) {
					if (Set.class.equals(this.type)) {
						property.addAll((Set<?>) value);
					} else {
						property.add(value);
					}
				}
			} catch (IllegalArgumentException e) {
				throw new AssertionError(e);
			} catch (IllegalAccessException e) {
				IllegalAccessError error;
				error = new IllegalAccessError(e.getMessage());
				error.initCause(e);
				throw error;
			} catch (InvocationTargetException e) {
				try {
					throw e.getCause();
				} catch (Error error) {
					throw error;
				} catch (RuntimeException runtime) {
					throw runtime;
				} catch (RepositoryException repository) {
					throw repository;
				} catch (Throwable throwable) {
					throw new UndeclaredThrowableException(throwable);
				}
			}
		}
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Class<?> type = message.getMethod().getReturnType();
		if (Void.TYPE.equals(type)) {
			if (Set.class.equals(message.getMethod().getParameterTypes()[0])) {
				property.setAll((Set<?>) message.getParameters()[0]);
			} else {
				property.setSingle(message.getParameters()[0]);
			}
			return message.proceed();
		} else if (Set.class.equals(type)) {
			return property.getAll();
		} else if (type.isPrimitive()) {
			Object result = property.getSingle();
			if (result == null)
				return message.proceed();
			return result;
		} else {
			try {
				Object result = type.cast(property.getSingle());
				if (result == null)
					return message.proceed();
				return result;
			} catch (ClassCastException e) {
				throw new ClassCastException(property.getSingle() + " cannot be cast to " + type.getName());
			}
		}
	}
}