package org.openrdf.repository.object.advisers;

import java.lang.reflect.Field;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FieldBehaviour implements Advice, Mergeable,
		Refreshable, PropertyConsumer {
	private final Logger logger = LoggerFactory.getLogger(FieldBehaviour.class);
	private final Class<?> concept;
	private final Class<?> type;
	private final Field field;
	private final PropertySet property;
	private final Object proxy;
	private volatile boolean populated;

	public FieldBehaviour(PropertySet property, Field field, Object proxy) {
		assert field != null;
		assert property != null;
		assert proxy != null;
		this.concept = field.getDeclaringClass();
		this.type = field.getType();
		this.field = field;
		this.property = property;
		this.proxy = proxy;
		try {
			field.setAccessible(true);
		} catch (SecurityException e) {
			logger.warn(e.toString(), e);
		}
	}

	public void usePropertyBindings(String binding, List<BindingSet> results) {
		if (property instanceof PropertyConsumer) {
			String var = binding + "_" + field.getName();
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
				Object value = field.get(source);
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
			}
		}
	}

	public Object intercept(ObjectMessage message) throws Throwable {
		if (populated)
			return message.proceed();
		Object fieldValue = getFieldValue();
		try {
			populateFields(fieldValue);
		} catch (IllegalAccessException e) {
			throw error(e);
		}
		try {
			populated = true;
			return message.proceed();
		} finally {
			populated = false;
			try {
				storeFields(fieldValue);
			} catch (IllegalAccessException e) {
				throw error(e);
			}
		}
	}

	private IllegalAccessError error(IllegalAccessException e) {
		IllegalAccessError error = new IllegalAccessError(e.getMessage());
		error.initCause(e);
		return error;
	}

	private void populateFields(Object fieldValue)
			throws IllegalAccessException {
		if (concept.isAssignableFrom(proxy.getClass())) {
			field.set(proxy, fieldValue);
		}
		for (Field f : proxy.getClass().getDeclaredFields()) {
			if (concept.isAssignableFrom(f.getType())) {
				Object behaviour = f.get(proxy);
				if (behaviour != null) {
					field.set(behaviour, fieldValue);
				}
			}
		}
	}

	private void storeFields(Object populated) throws IllegalAccessException {
		if (concept.isAssignableFrom(proxy.getClass())) {
			Object get = field.get(proxy);
			if (get != populated) {
				setFieldValue(get);
			}
		}
		for (Field f : proxy.getClass().getDeclaredFields()) {
			if (concept.isAssignableFrom(f.getType())) {
				Object behaviour = f.get(proxy);
				if (behaviour != null) {
					Object get = field.get(behaviour);
					if (get != populated) {
						setFieldValue(get);
					}
				}
			}
		}
	}

	private Object getFieldValue() {
		if (Set.class.equals(type))
			return property.getAll();
		Object single = property.getSingle();
		if (single == null)
			return nil(type);
		return single;
	}

	private void setFieldValue(Object get) {
		if (Set.class.equals(type))
			property.setAll((Set<?>) get);
		property.setSingle(get);
	}

	private Object nil(Class<?> type) {
		if (!type.isPrimitive())
			return null;
		if (Void.TYPE.equals(type))
			return null;
		if (Boolean.TYPE.equals(type))
			return Boolean.FALSE;
		if (Character.TYPE.equals(type))
			return Character.valueOf((char) 0);
		if (Byte.TYPE.equals(type))
			return Byte.valueOf((byte) 0);
		if (Short.TYPE.equals(type))
			return Short.valueOf((short) 0);
		if (Integer.TYPE.equals(type))
			return Integer.valueOf((int) 0);
		if (Long.TYPE.equals(type))
			return Long.valueOf((long) 0);
		if (Float.TYPE.equals(type))
			return Float.valueOf((float) 0);
		if (Double.TYPE.equals(type))
			return Double.valueOf((double) 0);
		throw new AssertionError();
	}
}