package org.openrdf.http.object.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericType<T> {
	private Class<T> type;
	private Type gtype;

	public GenericType(Class<T> type, Type genericType) {
		this.type = type;
		this.gtype = genericType;
	}

	public Class<?> clas() {
		return type;
	}

	public Type type() {
		return gtype;
	}

	public boolean is(Type type) {
		return type.equals(gtype);
	}

	public boolean isUnknown() {
		return type == null || Object.class.equals(type);
	}

	public boolean isMap() {
		return Map.class.equals(type);
	}

	public boolean isSet() {
		return Set.class.equals(type);
	}

	public boolean isArray() {
		return type != null && type.isArray();
	}

	public boolean isSetOrArray() {
		return isSet() || isArray();
	}

	public boolean isSetOf(Type componentType) {
		if (!isSet())
			return false;
		return componentType.equals(getComponentType());
	}

	public boolean isArrayOf(Type componentType) {
		if (!isArray())
			return false;
		return componentType.equals(getComponentType());
	}

	public boolean isSetOrArrayOf(Type componentType) {
		return isSetOf(componentType) || isArrayOf(componentType);
	}

	public boolean isOrIsSetOf(Type type) {
		if (is(type))
			return true;
		if (!isSet())
			return false;
		return type.equals(getComponentType());
	}

	public boolean isKeyUnknownOr(Type type) {
		Type key = getKeyType();
		return key == null || Object.class.equals(key) || key.equals(type);
	}

	public GenericType<?> getComponentGenericType() {
		return new GenericType(getComponentClass(), getComponentType());
	}

	public Class<?> getComponentClass() {
		return toClass(getComponentType());
	}

	public Type getComponentType() {
		if (isArray())
			return type.getComponentType();
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return args[args.length-1];
		}
		if (isSet() || isMap())
			return Object.class;
		return null;
	}

	public GenericType getKeyGenericType() {
		return new GenericType(getKeyClass(), getKeyType());
	}

	public Type getKeyType() {
		if (isArray())
			return type.getComponentType();
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			return args[0];
		}
		if (isSet() || isMap())
			return Object.class;
		return null;
	}

	public Class<?> getKeyClass() {
		return toClass(getKeyType());
	}

	public T nil() {
		if (isSet())
			return type.cast(Collections.emptySet());
		if (isArray())
			return type.cast(Array.newInstance(getComponentClass(), 0));
		if (isMap())
			return type.cast(Collections.emptyMap());
		return null;
	}

	public T cast(Object obj) {
		if (obj == null)
			return nil();
		return type.cast(obj);
	}

	public T castComponent(Object obj) {
		if (obj == null)
			return nil();
		if (isSet()) {
			return type.cast(Collections.singleton(obj));
		} else if (isArray()) {
			Object result = Array.newInstance(getComponentClass(), 1);
			Array.set(result, 0, obj);
			return type.cast(result);
		}
		return type.cast(obj);
	}

	public T castSet(Set<?> set) {
		return castCollection(set);
	}

	public T castArray(Object ar) {
		if (ar == null || Array.getLength(ar) == 0)
			return nil();
		if (isSet()) {
			int len = Array.getLength(ar);
			Set set = new HashSet(len);
			for (int i = 0; i < len; i++) {
				set.add(Array.get(ar, i));
			}
			return type.cast(set);
		} else if (isArray()) {
			return type.cast(ar);
		}
		return type.cast(Array.get(ar, 0));
	}

	public T castCollection(Collection<?> list) {
		if (list == null || list.isEmpty())
			return nil();
		if (isSet()) {
			if (list instanceof Set)
				return type.cast(list);
			return type.cast(new LinkedHashSet(list));
		} else if (isArray()) {
			int len = list.size();
			Object result = Array.newInstance(getComponentClass(), len);
			Iterator iter = list.iterator();
			for (int i = 0; i < len; i++) {
				Array.set(result, i, iter.next());
			}
			return type.cast(result);
		}
		Iterator<?> iter = list.iterator();
		return type.cast(iter.next());
	}

	public T castMap(Map<?, Collection<?>> map) {
		if (map == null || map.isEmpty())
			return nil();
		if (isMap()) {
			GenericType keyType = getKeyGenericType();
			GenericType<?> valueType = getComponentGenericType();
			Map result = new LinkedHashMap();
			for (Map.Entry<?, Collection<?>> e : map.entrySet()) {
				Object key = keyType.cast(e.getKey());
				Object value = valueType.castCollection(e.getValue());
				result.put(key, value);
			}
			return type.cast(result);
		}
		List list = new ArrayList();
		for (Map.Entry<?, Collection<?>> e : map.entrySet()) {
			list.addAll(e.getValue());
		}
		return castCollection(list);
	}

	public Iterator<?> iteratorOf(Object obj) {
		if (isSet())
			return ((Set) obj).iterator();
		if (isArray()) {
			int len = Array.getLength(obj);
			List<Object> list = new ArrayList<Object>(len);
			for (int i = 0; i < len; i++) {
				list.add(Array.get(obj, i));
			}
			return list.iterator();
		}
		return Collections.singleton(obj).iterator();
	}

	public String toString() {
		if (gtype != null)
			return gtype.toString();
		if (type != null)
			return type.toString();
		return "Unknown";
	}

	private Class<?> toClass(Type type) {
		if (type == null)
			return null;
		if (type instanceof Class)
			return (Class<?>) type;
		if (type instanceof GenericArrayType) {
			GenericArrayType atype = (GenericArrayType) type;
			Class<?> componentType = toClass(atype.getGenericComponentType());
			return Array.newInstance(toClass(componentType), 0).getClass();
		}
		if (type instanceof ParameterizedType) {
			return toClass(((ParameterizedType) type).getRawType());
		}
		return null;
	}

}
