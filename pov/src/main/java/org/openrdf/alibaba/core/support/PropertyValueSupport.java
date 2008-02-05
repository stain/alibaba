package org.openrdf.alibaba.core.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.core.PropertyValueBehaviour;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.InternalServerErrorException;
import org.openrdf.alibaba.exceptions.NotFoundException;
import org.openrdf.elmo.annotations.rdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements generic getter and setter methods for a Property. The bean can
 * either have the property URI declared on the getter method with an
 * {@link rdf} annotation or the property URI can have the syntax
 * <code>"java:" + className + "#" + propertyName</code>.
 * 
 * @author James Leigh
 * 
 */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public class PropertyValueSupport implements PropertyValueBehaviour {
	private static final Logger logger = LoggerFactory
			.getLogger(PropertyValueSupport.class);

	private static ConcurrentMap<QName, Method> getters = new ConcurrentHashMap<QName, Method>();

	private static ConcurrentMap<QName, Method> setters = new ConcurrentHashMap<QName, Method>();

	private Property property;

	public PropertyValueSupport(Property property) {
		this.property = property;
	}

	public void setValueOf(Object bean, Object value) throws AlibabaException {
		QName qname = property.getQName();
		Method setter = setters.get(qname);
		if (setter == null
				|| !setter.getDeclaringClass()
						.isAssignableFrom(bean.getClass())) {
			String pred = qname.getNamespaceURI() + qname.getLocalPart();
			setter = findWriteMethod(pred, bean.getClass().getInterfaces());
			if (setter == null) {
				setter = findWriteMethod(pred, bean.getClass());
				if (setter == null)
					throw new IllegalArgumentException(pred + " on " + bean);
			}
			setters.putIfAbsent(qname, setter);
		}
		try {
			Class<?> type = setter.getParameterTypes()[0];
			setter.invoke(bean, convert(value, type));
		} catch (IllegalArgumentException e) {
			throw new InternalServerErrorException(e);
		} catch (IllegalAccessException e) {
			throw new InternalServerErrorException(e);
		} catch (InvocationTargetException e) {
			throw new InternalServerErrorException(e);
		}
	}

	private <T> Object convert(Object value, Class<T> type) {
		if (value == null)
			return null;
		if (type.isInstance(value))
			return value;
		if (Collection.class.isAssignableFrom(type)) {
			if (value instanceof Collection<?>) {
				Collection coll = (Collection) value;
				if (type.isAssignableFrom(Set.class))
					return new HashSet(coll);
				if (type.isAssignableFrom(List.class))
					return new ArrayList(coll);
				if (!type.isInterface()) {
					try {
						Collection set = (Collection) type.newInstance();
						set.addAll(coll);
						return set;
					} catch (InstantiationException e) {
					} catch (IllegalAccessException e) {
					}
				}
				if (coll.isEmpty())
					return null;
			}
			return Collections.singleton(value);
		}
		if (value instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty())
				return null;
			return collection.toArray()[0];
		}
		return value;
	}

	private Method findWriteMethod(String pred, Class<?>[] interfaces)
			throws AlibabaException {
		Method getter = findReadMethod(pred, interfaces);
		return findWriteMethod(getter);
	}

	private Method findWriteMethod(String pred, Class<? extends Object> c)
			throws AlibabaException {
		Method getter = findReadMethod(pred, c);
		return findWriteMethod(getter);
	}

	private Method findWriteMethod(Method getter) throws NotFoundException {
		Class<?> declaring = getter.getDeclaringClass();
		String name = getter.getName();
		Class<?> type = getter.getReturnType();
		try {
			if (name.startsWith("is")) {
				return declaring.getMethod("set" + name.substring(2), type);
			}
			assert name.startsWith("get");
			return declaring.getMethod("set" + name.substring(3), type);
		} catch (NoSuchMethodException e) {
			throw new NotFoundException(e);
		}
	}

	public Object getValueOf(Object bean) throws AlibabaException {
		if (bean == null)
			return null;
		QName qname = property.getQName();
		Method getter = getters.get(qname);
		if (getter == null) {
			String pred = qname.getNamespaceURI() + qname.getLocalPart();
			getter = findReadMethod(pred, bean.getClass().getInterfaces());
			if (getter == null) {
				getter = findReadMethod(pred, bean.getClass());
				if (getter == null) {
					logger.warn("Cannot find property {} in class {}",
							property, bean.getClass().getSimpleName());
				}
			}
			if (getter != null) {
				getters.putIfAbsent(qname, getter);
			}
		}
		if (getter == null
				|| !getter.getDeclaringClass()
						.isAssignableFrom(bean.getClass()))
			return null;
		try {
			return getter.invoke(bean);
		} catch (Exception e) {
			throw new InternalServerErrorException(e);
		}
	}

	private Method findReadMethod(String pred, Class<?>[] interfaces) {
		for (Class<?> c : interfaces) {
			Method m = findReadMethod(pred, c);
			if (m != null)
				return m;
		}
		return null;
	}

	private Method findReadMethod(String pred, Class<?> c) {
		for (Method method : c.getMethods()) {
			if (method.getParameterTypes().length > 0)
				continue;
			if (!method.isAnnotationPresent(rdf.class))
				continue;
			String uri = method.getAnnotation(rdf.class).value()[0];
			if (uri.equals(pred))
				return method;
		}
		StringBuffer property = getClassInitProperty(pred, c.getName());
		if (property != null) {
			property.insert(0, "get");
			Method method = getMethod(c, property.toString());
			if (method != null)
				return method;
			property.replace(0, 3, "is");
			method = getMethod(c, property.toString());
			if (method != null)
				return method;
		}
		Class<?> sc = c.getSuperclass();
		if (sc == null)
			return null;
		return findReadMethod(pred, sc);
	}

	private StringBuffer getClassInitProperty(String pred, String cname) {
		if (!pred.startsWith("java:"))
			return null;
		int h = 5 + cname.length();
		if (h > pred.length() - 2)
			return null;
		if (pred.charAt(h) != '#')
			return null;
		if (pred.indexOf(cname, 5) != 5)
			return null;
		StringBuffer sb = new StringBuffer(pred.length() - h + 3);
		sb.append(Character.toUpperCase(pred.charAt(h + 1)));
		sb.append(pred.substring(h + 2));
		return sb;
	}

	private Method getMethod(Class<?> c, String name) {
		try {
			return c.getMethod(name);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

}
