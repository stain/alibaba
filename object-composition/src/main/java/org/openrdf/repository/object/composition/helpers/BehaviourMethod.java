/*
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.precedes;

/**
 * Represents an aspect in a behaviour class.
 *
 * @author James Leigh
 **/
public class BehaviourMethod {
	private final Class<?> javaClass;
	private final Method method;

	public BehaviourMethod(Class<?> behaviour, Method method) {
		assert behaviour != null;
		assert method != null;
		this.javaClass = behaviour;
		this.method = method;
	}

	private BehaviourMethod(Class<?> behaviour) {
		this.javaClass = behaviour;
		this.method = null;
	}

	public Class<?> getBehaviour() {
		return javaClass;
	}

	public Method getMethod() {
		return method;
	}

	public boolean isMessage() {
		return method.isAnnotationPresent(parameterTypes.class);
	}

	public boolean isEmptyOverridesPresent() {
		precedes ann = javaClass.getAnnotation(precedes.class);
		if (ann == null)
			return false;
		Class<?>[] values = ann.value();
		return values != null && values.length == 0;
	}

	public boolean isOverridesPresent() {
		return javaClass.isAnnotationPresent(precedes.class);
	}

	public boolean overrides(BehaviourMethod b1,
			boolean explicit, Collection<Class<?>> exclude) {
		if (b1.getBehaviour().equals(javaClass))
			return false;
		if (exclude.contains(javaClass))
			return false;
		exclude.add(javaClass);
		precedes ann = javaClass.getAnnotation(precedes.class);
		if (ann == null)
			return false;
		Class<?>[] values = ann.value();
		for (Class<?> c : values) {
			if (c.equals(b1.getBehaviour()))
				return true;
			BehaviourMethod cbm = new BehaviourMethod(c);
			if (c.isAssignableFrom(b1.getBehaviour()))
				return explicit || !b1.overrides(cbm, true, new HashSet<Class<?>>());
			if (cbm.overrides(b1, explicit, exclude))
				return explicit || !b1.overrides(cbm, true, new HashSet<Class<?>>());
		}
		return false;
	}
}
