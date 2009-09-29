/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.trigger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.repository.object.annotations.iri;

/**
 * Helper class for tracking connection triggers.
 * 
 * @author James Leigh
 *
 */
public class Trigger {

	private Class<?> declaredIn;

	private String methodName;

	private Class<?>[] types;

	private List<String> parameters;

	public Trigger(Method method) {
		this.declaredIn = method.getDeclaringClass();
		this.methodName = method.getName();
		this.types = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		parameters = Arrays.asList(new String[anns.length]);
		for (int i = 0; i < anns.length; i++) {
			for (Annotation ann : anns[i]) {
				if (ann instanceof iri) {
					parameters.set(i, ((iri) ann).value());
				}
			}
		}
	}

	public String toString() {
		return methodName;
	}

	public Class<?> getDeclaredIn() {
		return declaredIn;
	}

	public String getMethodName() {
		return methodName;
	}

	public Class<?>[] getParameterTypes() {
		return types;
	}

	public int getParameterIndex(URI pred) {
		if (parameters.isEmpty())
			return -1;
		if (parameters.size() < 2)
			return 0;
		return parameters.indexOf(pred.stringValue());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + Arrays.hashCode(types);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trigger other = (Trigger) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (!Arrays.equals(types, other.types))
			return false;
		return true;
	}

}
