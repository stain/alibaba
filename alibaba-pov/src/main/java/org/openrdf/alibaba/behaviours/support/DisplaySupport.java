package org.openrdf.alibaba.behaviours.support;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.behaviours.DisplayBehaviour;
import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.exceptions.InternalServerErrorException;
import org.openrdf.alibaba.exceptions.NotImplementedException;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Alt;
import org.openrdf.concepts.rdf.Bag;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdf.Seq;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@rdf(POV.NS + "Display")
public class DisplaySupport implements DisplayBehaviour {
	private final Logger logger = LoggerFactory.getLogger(DisplaySupport.class);

	private static ConcurrentMap<QName, Method> getters = new ConcurrentHashMap<QName, Method>();

	private static ConcurrentMap<QName, Method> setters = new ConcurrentHashMap<QName, Method>();

	private Display display;

	public DisplaySupport(Display display) {
		this.display = display;
	}

	public Set<?> getValuesOf(Object resource) throws AlibabaException {
		Container<Property> props = display.getPovProperties();
		if (props == null) {
			return Collections.singleton(resource);
		} else if (props instanceof Seq) {
			List<Object> list = new ArrayList<Object>();
			for (Property prop : props) {
				Object value = getPropertyValue(resource, prop.getQName());
				if (value instanceof Set) {
					Set<?> set = (Set<?>) value;
					int size = set.size();
					if (size == 0) {
						list.add(null);
					} else if (size == 1) {
						list.addAll(set);
					} else {
						list.add(set.toArray()[0]);
					}
				} else {
					list.add(value);
				}
			}
			return Collections.singleton(list.toArray());
		} else if (props instanceof Alt) {
			Object value = null;
			for (Property prop : props) {
				value = getPropertyValue(resource, prop.getQName());
				boolean empty = value instanceof Set && ((Set) value).isEmpty();
				if (value != null && !empty)
					break;
			}
			close(props);
			if (value instanceof Set) {
				return (Set) value;
			}
			if (value == null)
				return Collections.EMPTY_SET;
			return Collections.singleton(value);
		} else {
			assert props instanceof Bag : props;
			Set<Object> values = new HashSet<Object>();
			for (Property prop : props) {
				Object value = getPropertyValue(resource, prop.getQName());
				if (value instanceof Set) {
					values.addAll((Set<?>) value);
				} else if (value != null) {
					values.add(value);
				}
			}
			return values;
		}
	}

	public void setValuesOf(Object resource, Set<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Container<Property> props = display.getPovProperties();
			if (props == null || props instanceof Bag) {
				throw new BadRequestException("Cannot set values: " + values
						+ " for: " + display);
			} else if (props instanceof Seq) {
				Iterator<?> iter = values.iterator();
				for (Property prop : props) {
					if (iter.hasNext()) {
						setPropertyValue(resource, prop.getQName(), iter.next());
					}
				}
			} else {
				assert props instanceof Alt : props;
				Iterator<Property> iter = props.iterator();
				assert iter.hasNext() : props;
				QName name = iter.next().getQName();
				close(props);
				setPropertyValue(resource, name, values);
			}
		}
	}

	private void setPropertyValue(Object bean, QName property, Object value)
			throws AlibabaException {
		Method setter = setters.get(property);
		if (setter == null
				|| !setter.getDeclaringClass()
						.isAssignableFrom(bean.getClass())) {
			String pred = property.getNamespaceURI() + property.getLocalPart();
			setter = findWriteMethod(pred, bean.getClass().getInterfaces());
			if (setter == null) {
				setter = findWriteMethod(pred, bean.getClass());
				if (setter == null)
					throw new NotImplementedException(pred + " on " + bean);
			}
			setters.putIfAbsent(property, setter);
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
		if (type.equals(Set.class) && !(value instanceof Set<?>)) {
			return Collections.singleton(value);
		} else if (!type.equals(Set.class) && value instanceof Set<?>) {
			return ((Set<?>) value).toArray()[0];
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

	private Method findWriteMethod(Method getter)
			throws NotImplementedException {
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
			throw new NotImplementedException(e);
		}
	}

	private void close(Container<Property> props) {
		if (props instanceof Closeable) {
			try {
				((Closeable) props).close();
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}

	private Object getPropertyValue(Object bean, QName property)
			throws AlibabaException {
		Method getter = getters.get(property);
		if (getter == null) {
			String pred = property.getNamespaceURI() + property.getLocalPart();
			getter = findReadMethod(pred, bean.getClass().getInterfaces());
			if (getter == null) {
				getter = findReadMethod(pred, bean.getClass());
				if (getter == null) {
					logger.warn("Cannot find property {} in class {}",
							property, bean.getClass().getSimpleName());
				}
			}
			if (getter != null) {
				getters.putIfAbsent(property, getter);
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
