/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.http.object.util;

import java.lang.annotation.Annotation;
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

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.annotations.iri;

/**
 * Utility class for dealing with generic types.
 * 
 * @author James Leigh
 */
public class MessageType {
	private String mimeType;
	private ObjectConnection con;
	private Class<?> type;
	private Type gtype;

	@Deprecated
	public MessageType(Class<?> type, Type genericType, String mimeType,
			ObjectConnection con) {
		this(mimeType, type, genericType, con);
	}

	public MessageType(String media, Class<?> ptype, Type gtype,
			ObjectConnection con) {
		assert con != null;
		this.type = ptype;
		this.gtype = gtype;
		this.mimeType = media;
		this.con = con;
	}

	public String getMimeType() {
		return mimeType;
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public ObjectFactory getObjectFactory() {
		return con.getObjectFactory();
	}

	public ValueFactory getValueFactory() {
		return con.getValueFactory();
	}

	public boolean isConcept(Class<?> component) {
		for (Annotation ann : component.getAnnotations()) {
			if (ann.annotationType().isAnnotationPresent(iri.class))
				return true;
		}
		return getObjectFactory().isNamedConcept(component);
	}

	public boolean isDatatype(Class<?> type2) {
		return getObjectFactory().isDatatype(type2);
	}

	public boolean isPrimitive() {
		return clas().isPrimitive();
	}

	public boolean isAssignableFrom(Class<?> type) {
		return clas().isAssignableFrom(type);
	}

	public boolean isObject() {
		return clas().equals(Object.class);
	}

	public boolean is(Class<?> type) {
		return clas().equals(type);
	}

	public boolean isOctetStream() {
		return mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType);
	}

	public boolean isText() {
		return mimeType != null && mimeType.startsWith("text/");
	}

	public boolean isComponentType(Class<?> type) {
		return getComponentClass().equals(type);
	}

	public boolean isByteArray() {
		return isArray() && Byte.TYPE.equals(getComponentType());
	}

	public MessageType as(Class<?> t) {
		return new MessageType(mimeType, t, t, con);
	}

	public MessageType key(String mimetype) {
		MessageType kt = getKeyGenericType();
		return new MessageType(mimetype, kt.clas(), kt.type(), con);
	}

	public MessageType component() {
		MessageType gtype = getComponentGenericType();
		return new MessageType(mimeType, gtype.clas(), gtype.type(), con);
	}

	public MessageType component(String mimetype) {
		MessageType vt = getComponentGenericType();
		return new MessageType(mimetype, vt.clas(), vt.type(), con);
	}

	public MessageType as(String mimetype) {
		return new MessageType(mimeType, clas(), type(), con);
	}

	public MessageType as(Class<?> clas, Type type) {
		return new MessageType(mimeType, clas, type, con);
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

	public MessageType getComponentGenericType() {
		return new MessageType(mimeType, getComponentClass(),
				getComponentType(), con);
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
			return args[args.length - 1];
		}
		if (isSet() || isMap())
			return Object.class;
		return null;
	}

	public MessageType getKeyGenericType() {
		return new MessageType(mimeType, getKeyClass(), getKeyType(), con);
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

	public Object nil() {
		if (isSet())
			return type.cast(Collections.emptySet());
		if (isArray())
			return type.cast(Array.newInstance(getComponentClass(), 0));
		if (isMap())
			return type.cast(Collections.emptyMap());
		return null;
	}

	public Object cast(Object obj) {
		if (obj == null)
			return nil();
		return type.cast(obj);
	}

	public Object castComponent(Object obj) {
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

	public Object castSet(Set<?> set) {
		return castCollection(set);
	}

	public Object castArray(Object ar) {
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

	public Object castCollection(Collection<?> list) {
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

	public Object castMap(Map<?, Collection<?>> map) {
		if (map == null || map.isEmpty())
			return nil();
		if (isMap()) {
			MessageType keyType = getKeyGenericType();
			MessageType valueType = getComponentGenericType();
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
		return Object.class; // wildcard
	}

}
