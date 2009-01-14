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
package org.openrdf.elmo.rolemapper;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openrdf.elmo.annotations.complementOf;
import org.openrdf.elmo.annotations.intersectionOf;

/**
 * Tracks recorded roles and maps them to their subject type.
 * 
 * @author James Leigh
 * 
 */
public class ComplexMapper<URI> {
	private Map<Class<?>, AnnotatedElement> complements;

	private Map<Class<?>, AnnotatedElement> intersections;

	public ComplexMapper() {
		complements = new ConcurrentHashMap<Class<?>, AnnotatedElement>();
		intersections = new ConcurrentHashMap<Class<?>, AnnotatedElement>();
	}

	public synchronized void recordRole(Class<?> role, AnnotatedElement elm) {
		if (elm.isAnnotationPresent(complementOf.class)) {
			complements.put(role, elm);
		}
		if (elm.isAnnotationPresent(intersectionOf.class)) {
			intersections.put(role, elm);
		}
	}

	public Collection<Class<?>> findAdditonalRoles(Collection<Class<?>> roles) {
		if (complements.isEmpty())
			return roles;
		Collection<Class<?>> result = new ArrayList<Class<?>>(roles.size() * 2);
		result.addAll(roles);
		addIntersectionsAndComplements(result);
		return result;
	}

	private void addIntersectionsAndComplements(Collection<Class<?>> roles) {
		for (Map.Entry<Class<?>, AnnotatedElement> e : intersections.entrySet()) {
			Class<?> inter = e.getKey();
			AnnotatedElement elm = e.getValue();
			Class<?>[] of = elm.getAnnotation(intersectionOf.class).value();
			if (!roles.contains(inter) && intersects(roles, of)) {
				roles.add(inter);
			}
		}
		boolean complementAdded = false;
		for (Map.Entry<Class<?>, AnnotatedElement> e : complements.entrySet()) {
			Class<?> comp = e.getKey();
			AnnotatedElement elm = e.getValue();
			Class<?> of = elm.getAnnotation(complementOf.class).value();
			if (!roles.contains(comp) && !contains(roles, of)) {
				complementAdded = true;
				roles.add(comp);
			}
		}
		if (complementAdded) {
			for (Map.Entry e : intersections.entrySet()) {
				Class<?> inter = (Class<?>) e.getKey();
				AnnotatedElement elm = (AnnotatedElement) e.getValue();
				Class<?>[] of = elm.getAnnotation(intersectionOf.class).value();
				if (!roles.contains(inter) && intersects(roles, of)) {
					roles.add(inter);
				}
			}
		}
	}

	private boolean intersects(Collection<Class<?>> roles, Class<?>[] ofs) {
		for (Class<?> of : ofs) {
			if (!contains(roles, of))
				return false;
		}
		return true;
	}

	private boolean contains(Collection<Class<?>> roles, Class<?> of) {
		for (Class<?> type : roles) {
			if (of.isAssignableFrom(type))
				return true;
		}
		return false;
	}
}