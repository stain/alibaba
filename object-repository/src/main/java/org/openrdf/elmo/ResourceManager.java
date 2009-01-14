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
package org.openrdf.elmo;

import java.util.Iterator;

/**
 * Manages the life-cycle of the RDF resource in side the Elmo JavaBean.
 * 
 * @author James Leigh
 * 
 */
public interface ResourceManager<Resource> {

	/**
	 * Determine what concepts and behaviours this resource represents.
	 * 
	 * @param resource
	 * @return Array of roles for this resource.
	 */
	public abstract Class<?> getEntityClass(Resource resource);

	/**
	 * If this concept is new to this resource merge them and return the
	 * combined set of concepts and behaviours this resource now represents,
	 * otherwise return the existing roles.
	 * 
	 * @param resource
	 * @param concept
	 * @return Array of roles for this resource.
	 */
	public abstract Class<?> mergeRole(Resource resource, Class<?> concept,
			Class<?>... concepts);

	/**
	 * Return the EntityClass used for this concept and assign this concept type
	 * to the resource.
	 * 
	 * @param resource
	 * @param concept
	 * @return Array of roles for this resource.
	 */
	public abstract Class<?> persistRole(Resource resource, Class<?> concept,
			Class<?>... concepts);

	/**
	 * Removes a given role from the resource and returns the new role set for
	 * this resource.
	 * 
	 * @param resource
	 * @param concept
	 * @return Array of roles for this resource, without the given concept
	 */
	public abstract Class<?> removeRole(Resource resource, Class<?>... concepts);

	/**
	 * Change all references of <code>before</code> to <code>after</code>.
	 * 
	 * @param before
	 * @param after
	 */
	public abstract void renameResource(Resource before, Resource after);

	/**
	 * Remove this resource from the repository.
	 * 
	 * @param resource
	 */
	public abstract void removeResource(Resource resource);

	/**
	 * Creates a query that will return all resource that implement this
	 * concept.
	 * 
	 * @param concept
	 * @return collection of resources.
	 */
	public abstract Iterator<Resource> createRoleQuery(Class<?> concept);

}