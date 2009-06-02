/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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
import java.util.List;

import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.concepts.Message;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Implements the Message interface(s) through an InvocationHandler.
 * 
 * @author James Leigh
 *
 */
public class InvocationMessageContext implements InvocationHandler, Message {

	private Object target;

	private Method method;

	private Object[] parameters;

	private List<Object> invokeTarget = new ArrayList<Object>();

	private List<Method> invokeMethod = new ArrayList<Method>();

	private int count;

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
		if (method.getDeclaringClass().equals(Message.class)) {
			return method.invoke(this, args);
		}
		String uri = method.getAnnotation(rdf.class).value();
		if (uri.equals(OBJ.PROCEED.stringValue())) {
			return proceed();
		} else if (uri.equals(OBJ.TARGET.stringValue())) {
			return getTarget();
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

	public Object getTarget() {
		return target;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public Object proceed() throws Exception {
		try {
			if (count >= invokeTarget.size())
				return nil(method.getReturnType());
			Method im = invokeMethod.get(count);
			Object it = invokeTarget.get(count);
			count++;
			Class<?>[] param = im.getParameterTypes();
			if (param.length == 1 && isMessageType(param[0]))
				return im.invoke(it, as(param[0]));
			Object result = im.invoke(it, getParameters());
			if (isNil(result, method.getReturnType()))
				return proceed();
			return result;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception)
				throw (Exception) cause;
			if (cause instanceof Error)
				throw (Error) cause;
			throw e;
		}
	}

	private <T> T as(Class<T> type) {
		ClassLoader cl = type.getClassLoader();
		Class<?>[] types = new Class<?>[] { type };
		return type.cast(Proxy.newProxyInstance(cl, types, this));
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
		rdf ann = type.getAnnotation(rdf.class);
		if (ann != null && OBJ.MESSAGE.stringValue().equals(ann.value()))
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
				if (anns[i][j].annotationType().equals(rdf.class)) {
					if (((rdf) anns[i][j]).value().equals(uri)) {
						return i;
					}
				}
			}
		}
		throw new UnsupportedOperationException("Parameter not found: " + uri);
	}

}