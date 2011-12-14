/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object.composition.helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openrdf.annotations.iri;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.traits.BooleanMessage;
import org.openrdf.repository.object.traits.ByteMessage;
import org.openrdf.repository.object.traits.CharacterMessage;
import org.openrdf.repository.object.traits.DoubleMessage;
import org.openrdf.repository.object.traits.FloatMessage;
import org.openrdf.repository.object.traits.IntegerMessage;
import org.openrdf.repository.object.traits.LongMessage;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.openrdf.repository.object.traits.ShortMessage;
import org.openrdf.repository.object.traits.VoidMessage;
import org.openrdf.repository.object.vocabulary.MSG;

/**
 * Implements the Message interface(s) through an InvocationHandler.
 * 
 * @author James Leigh
 * 
 */
public class InvocationMessageContext implements InvocationHandler, ObjectMessage {
	/** @return the parameters in the same order as the method return type. */
	public static final String PARAMETERS = "getParameters";
	/** @return the response in the same form as the method return type. */
	public static final String PROCEED = "proceed";
	/** @return the response as a single literal Java object. */
	public static final String LITERAL_RESPONSE = "getFunctionalLiteralResponse";
	/** @return the response as a single Java object. */
	public static final String OBJECT_RESPONSE = "getFunctionalObjectResponse";
	/** @return the response as a set of Java objects. */
	public static final String SET_RESPONSE = "getObjectResponse";

	public static Class<?> selectMessageType(Class<?> returnType) {
		if (!returnType.isPrimitive())
			return ObjectMessage.class;
		if (Boolean.TYPE.equals(returnType))
			return BooleanMessage.class;
		if (Byte.TYPE.equals(returnType))
			return ByteMessage.class;
		if (Character.TYPE.equals(returnType))
			return CharacterMessage.class;
		if (Double.TYPE.equals(returnType))
			return DoubleMessage.class;
		if (Float.TYPE.equals(returnType))
			return FloatMessage.class;
		if (Integer.TYPE.equals(returnType))
			return IntegerMessage.class;
		if (Long.TYPE.equals(returnType))
			return LongMessage.class;
		if (Short.TYPE.equals(returnType))
			return ShortMessage.class;
		if (Void.TYPE.equals(returnType))
			return VoidMessage.class;
		throw new AssertionError("Unknown primitive: " + returnType);
	}

	private Object target;

	private Class<?> type;

	private final Method method;

	private Object[] parameters;

	private Set response;

	private final List<Object> invokeTarget = new ArrayList<Object>();

	private final List<Method> invokeMethod = new ArrayList<Method>();

	private int count;

	public InvocationMessageContext(Object target, Class<?> messageType,
			Method method, Object[] parameters) {
		this.target = target;
		if (isMessageType(messageType)) {
			this.type = messageType;
		}
		this.method = method;
		this.parameters = parameters;
	}

	public InvocationMessageContext(Object target, Method method,
			Object[] parameters) {
		this.target = target;
		this.method = method;
		this.parameters = parameters;
	}

	public InvocationMessageContext appendInvocation(Object target,
			Method method) {
		invokeTarget.add(target);
		invokeMethod.add(method);
		return this;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		if (declaringClass.equals(ObjectMessage.class)
				|| declaringClass.equals(Object.class)) {
			try {
				return method.invoke(this, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} else if (!method.isAnnotationPresent(iri.class)) {
			try {
				String n = method.getName();
				Class<?>[] p = method.getParameterTypes();
				return ObjectMessage.class.getMethod(n, p).invoke(this, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
		String uri = method.getAnnotation(iri.class).value();
		if (uri.equals(MSG.TARGET.stringValue())) {
			if (args == null || args.length == 0)
				return getMsgTarget();
			setMsgTarget(args[0]);
			return null;
		} else if (uri.equals(MSG.OBJECT_SET.stringValue())) {
			if (args == null || args.length == 0)
				return getObjectResponse();
			setObjectResponse((Set) args[0]);
			return null;
		} else if (uri.equals(MSG.LITERAL_SET.stringValue())) {
			if (args == null || args.length == 0)
				return getLiteralResponse();
			setLiteralResponse((Set) args[0]);
			return null;
		} else if (uri.equals(MSG.OBJECT.stringValue())) {
			if (args == null || args.length == 0)
				return getFunctionalObjectResponse();
			setFunctionalObjectResponse(args[0]);
			return null;
		} else if (uri.equals(MSG.LITERAL.stringValue())) {
			if (args == null || args.length == 0)
				return getFunctionalLiteralResponse();
			setFunctionalLiteralResponse(args[0]);
			return null;
		}
		int idx = getParameterIndex(uri);
		if (args == null || args.length == 0) {
			return getParameters()[idx];
		} else {
			Object[] params = getParameters();
			params[idx] = args[0];
			setParameters(params);
			return null;
		}
	}

	@Override
	public String toString() {
		String params = Arrays.asList(parameters).toString();
		String values = params.substring(1, params.length() - 1);
		return method.getName() + "(" + values + ")";
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public Object proceed() {
		response = nextResponse();
		if (method.getReturnType().equals(Set.class))
			return response;
		if (response.size() == 1) {
			Object result = response.iterator().next();
			if (result != null)
				return result;
		}
		return nil(method.getReturnType());
	}

	public Object getMsgTarget() {
		return target;
	}

	public void setMsgTarget(Object msgTarget) {
		this.target = msgTarget;
	}

	public Object getFunctionalLiteralResponse() {
		if (response == null) {
			response = nextResponse();
		}
		if (response.size() == 1) {
			Object result = response.iterator().next();
			if (result != null)
				return result;
		}
		return nil(method.getReturnType());
	}

	public void setFunctionalLiteralResponse(Object functionalLiteralResponse) {
		this.response = Collections.singleton(functionalLiteralResponse);
	}

	public Object getFunctionalObjectResponse() {
		if (response == null) {
			response = nextResponse();
		}
		if (response.size() == 1) {
			Object result = response.iterator().next();
			if (result != null)
				return result;
		}
		return nil(method.getReturnType());
	}

	public void setFunctionalObjectResponse(Object functionalObjectResponse) {
		this.response = Collections.singleton(functionalObjectResponse);
	}

	public Set<Object> getLiteralResponse() {
		if (response == null) {
			response = nextResponse();
		}
		return response;
	}

	public void setLiteralResponse(Set<?> literalResponse) {
		this.response = literalResponse;
	}

	public Set<Object> getObjectResponse() {
		if (response == null) {
			response = nextResponse();
		}
		return response;
	}

	public void setObjectResponse(Set<?> objectResponse) {
		this.response = objectResponse;
	}

	private Set<Object> nextResponse() {
		try {
			if (count >= invokeTarget.size())
				return Collections.emptySet();
			Method im = invokeMethod.get(count);
			Object it = invokeTarget.get(count);
			count++;
			// TODO check for @ParameterTypes
			Class<?>[] param = im.getParameterTypes();
			Object result;
			if (param.length == 1 && isMessageType(param[0])) {
				result = im.invoke(it, as(param[0], im.getReturnType()));
				if (isNil(result, im.getReturnType()))
					return Collections.emptySet();
			} else {
				result = im.invoke(it, getParameters(im));
				if (isNil(result, im.getReturnType()))
					return nextResponse();
			}
			if (im.getReturnType().equals(Set.class))
				return (Set) result;
			return Collections.singleton(result);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			if (cause instanceof Error)
				throw (Error) cause;
			throw new BehaviourException(cause);
		} catch (IllegalArgumentException e) {
			throw new BehaviourException(e);
		} catch (IllegalAccessException e) {
			throw new BehaviourException(e);
		}
	}

	private <T> T as(Class<T> type, Class<?> returnType) {
		if (this.type != null && type.isAssignableFrom(this.type)) {
			type = (Class<T>) this.type;
		}
		ClassLoader cl = type.getClassLoader();
		Class<?> selected = selectMessageType(returnType);
		if (selected.isAssignableFrom(type)
				|| ObjectMessage.class.isAssignableFrom(type)) {
			Class<?>[] types = new Class<?>[] { type };
			return type.cast(Proxy.newProxyInstance(cl, types, this));
		} else {
			Class<?>[] types = new Class<?>[] { type, selected };
			return type.cast(Proxy.newProxyInstance(cl, types, this));
		}
	}

	private boolean isNil(Object result, Class<?> type) {
		if (result == null)
			return true;
		if (!type.isPrimitive())
			return false;
		return result.equals(nil(type));
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

	private boolean isMessageType(Class<?> type) {
		if (!type.isInterface())
			return false;
		if (ObjectMessage.class.equals(type)
				|| BooleanMessage.class.equals(type)
				|| ByteMessage.class.equals(type)
				|| CharacterMessage.class.equals(type)
				|| DoubleMessage.class.equals(type)
				|| FloatMessage.class.equals(type)
				|| IntegerMessage.class.equals(type)
				|| LongMessage.class.equals(type)
				|| ShortMessage.class.equals(type)
				|| VoidMessage.class.equals(type))
			return true;
		iri ann = type.getAnnotation(iri.class);
		if (ann != null
				&& (MSG.MESSAGE.stringValue().equals(ann.value())))
			return true;
		for (Class<?> s : type.getInterfaces()) {
			if (isMessageType(s))
				return true;
		}
		return false;
	}

	private int getParameterIndex(String uri) {
		Annotation[][] anns = method.getParameterAnnotations();
		for (int i = 0; i < anns.length; i++) {
			for (int j = 0; j < anns[i].length; j++) {
				if (anns[i][j].annotationType().equals(iri.class)) {
					if (((iri) anns[i][j]).value().equals(uri)) {
						return i;
					}
				}
			}
		}
		throw new UnsupportedOperationException("Parameter not found: " + uri);
	}

	private Object[] getParameters(Method method) {
		Object[] parameters = getParameters();
		Annotation[][] anns = method.getParameterAnnotations();
		Object[] result = new Object[anns.length];
		for (int i = 0; i < anns.length; i++) {
			if (i < parameters.length) {
				// if no @rdf copy over parameter by position
				result[i] = parameters[i];
			}
			for (int j = 0; j < anns[i].length; j++) {
				if (anns[i][j].annotationType().equals(iri.class)) {
					String uri = ((iri) anns[i][j]).value();
					result[i] = parameters[getParameterIndex(uri)];
				}
			}
		}
		return result;
	}

}
