package org.openrdf.http.object.util;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GenericType<T> {
	private Class<T> type;
	private Type gtype;

	public GenericType(Class<T> type, Type genericType) {
		this.type = type;
		this.gtype = genericType;
	}

	public Class<?> getComponentClass() {
		Type type = getComponentType();
		if (type instanceof Class)
			return (Class<?>) type;
		return null;
	}

	public boolean is(Type type) {
		return gtype.equals(type);
	}

	public boolean isSet() {
		return Set.class.equals(type);
	}

	public boolean isArray() {
		return type.isArray();
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

	public boolean isOrIsSetOf(Type type) {
		if (is(type))
			return true;
		if (!isSet())
			return false;
		return type.equals(getComponentType());
	}

	public Type getComponentType() {
		if (isArray())
			return type.getComponentType();
		if (gtype instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) gtype;
			Type[] args = ptype.getActualTypeArguments();
			if (args.length == 1) {
				return args[0];
			}
		}
		if (isSet())
			return Object.class;
		return null;
	}

	public T nil() {
		if (isSet())
			return type.cast(Collections.emptySet());
		if (isArray())
			return type.cast(Array.newInstance(getComponentClass(), 0));
		return null;
	}

	public T cast(Object obj) {
		if (isSet()) {
			if (obj instanceof Set)
				return type.cast(obj);
			if (obj == null)
				return nil();
			if (obj instanceof Object[])
				return type.cast(new HashSet(Arrays.asList((Object[]) obj)));
			return type.cast(Collections.singleton(obj));
		}
		return type.cast(obj);
	}

}
