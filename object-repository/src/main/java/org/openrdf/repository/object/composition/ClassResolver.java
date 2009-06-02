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
package org.openrdf.repository.object.composition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openrdf.model.URI;
import org.openrdf.repository.object.managers.RoleMapper;

public class ClassResolver {
	private ClassCompositor compositor;
	private RoleMapper mapper;
	private Class<?> blank;
	private ConcurrentMap<URI, Object> individuals = new ConcurrentHashMap<URI, Object>();
	private ConcurrentMap<Collection<URI>, Class<?>> multiples = new ConcurrentHashMap<Collection<URI>, Class<?>>();

	public void setClassCompositor(ClassCompositor compositor) {
		this.compositor = compositor;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public void init() {
		Set<URI> emptySet = Collections.emptySet();
		blank = resolveBlankEntity(emptySet);
	}

	public Class<?> resolveBlankEntity() {
		return blank;
	}

	public Class<?> resolveBlankEntity(Collection<URI> types) {
		Class<?> proxy = multiples.get(types);
		if (proxy != null)
			return proxy;
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		proxy = compositor.resolveRoles(mapper.findRoles(types, roles));
		multiples.putIfAbsent(types, proxy);
		return proxy;
	}

	public Class<?> resolveEntity(URI resource) {
		if (resource != null && mapper.isIndividualRolesPresent(resource))
			return resolveIndividualEntity(resource, Collections.EMPTY_SET);
		return resolveBlankEntity();
	}

	public Class<?> resolveEntity(URI resource, Collection<URI> types) {
		if (resource != null && mapper.isIndividualRolesPresent(resource))
			return resolveIndividualEntity(resource, types);
		return resolveBlankEntity(types);
	}

	private Class<?> resolveIndividualEntity(URI resource, Collection<URI> types) {
		Object[] ar = (Object[]) individuals.get(resource);
		if (ar != null && types.equals(ar[0]))
			return (Class<?>) ar[1];
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		roles = mapper.findIndividualRoles(resource, roles);
		roles = mapper.findRoles(types, roles);
		Class<?> proxy = compositor.resolveRoles(roles);
		individuals.put(resource, new Object[] { types, proxy });
		return proxy;
	}

}
