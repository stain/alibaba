/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.interceptor.InvocationContext;

/**
 * Internal implementation of InvocationContext. This is passed to AroundInvoke
 * methods of registered behaviours.
 * 
 * @author James Leigh
 * 
 */
public class InvocationContextImpl implements InvocationContext {

	private Object target;

	private Method method;

	private Object[] parameters;

	private Method proceed;

	private List<Object> invokeTarget = new ArrayList<Object>();

	private List<Method> invokeMethod = new ArrayList<Method>();

	private int count;

	public InvocationContextImpl(Object target, Method method,
			Object[] parameters, Method proceed) {
		this.target = target;
		this.method = method;
		this.parameters = parameters;
		this.proceed = proceed;
	}

	public InvocationContextImpl appendInvocation(Object target, Method method) {
		invokeTarget.add(target);
		invokeMethod.add(method);
		return this;
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getContextData() {
		return Collections.EMPTY_MAP;
	}

	public Object proceed() throws Exception {
		try {
			if (count < invokeTarget.size()) {
				Method im = invokeMethod.get(count);
				Object it = invokeTarget.get(count);
				count++;
				Class<?>[] param = im.getParameterTypes();
				if (param.length == 1
						&& param[0].equals(InvocationContext.class))
					return im.invoke(it, this);
				return im.invoke(it, getParameters());
			}
			return proceed.invoke(target, parameters);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception)
				throw (Exception) cause;
			throw e;
		}
	}

}