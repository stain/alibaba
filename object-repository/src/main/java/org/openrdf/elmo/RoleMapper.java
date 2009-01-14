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

import java.util.Collection;

/**
 * Maps between roles and rdf:type.
 * 
 * @author James Leigh
 * 
 * @param <URI>
 *            Class used to represent a rdf:type.
 */
public interface RoleMapper<URI> {
	public abstract void setRdfTypeFactory(RdfTypeFactory<URI> vf);

	public abstract Collection<Class<?>> getConceptClasses();

	public abstract Collection<Class<?>> getConceptOnlyClasses();

	public abstract void addFactory(Class<?> factory);

	public abstract void addFactory(Class<?> factory, String type);

	public abstract void addConcept(Class<?> concept);

	public abstract void addConcept(Class<?> concept, String type);

	public abstract void addBehaviour(Class<?> behaviour);

	public abstract void addBehaviour(Class<?> behaviour, String type);

	/**
	 * Determines if a registered role hase this value as a {link
	 * org.openrdf.annotations.oneOf} value.
	 * 
	 * @param instance
	 * @return <code>true</code> if {link
	 *         {@link #findIndividualRoles(Object, Collection)} will modify the
	 *         collection.
	 */
	public boolean isIndividualRolesPresent(URI instance);

	/**
	 * Adds roles to the collection that are specific to this instance. Defined
	 * with the {link org.openrdf.annotations.oneOf} annotation.
	 * 
	 * @param instance
	 * @param classes
	 * @return <code>classes</code>
	 */
	public Collection<Class<?>> findIndividualRoles(URI instance,
			Collection<Class<?>> classes);

	/**
	 * Finds the Java Class for this rdf:Class. Searches for register classes.
	 * 
	 * @param type
	 */
	public abstract Collection<Class<?>> findRoles(URI type);

	/**
	 * Finds all the roles that should be implemented by these types.
	 * 
	 * @param types
	 *            rdf:types
	 * @param roles
	 *            collection should be used to add the classes.
	 * @return <code>roles</code>
	 */
	public abstract Collection<Class<?>> findRoles(Collection<URI> types,
			Collection<Class<?>> roles);

	/**
	 * Finds if there exists a Java Class for this rdf:Class. Searches for
	 * register classes.
	 * 
	 * @param type
	 */
	public abstract boolean isTypeRecorded(URI type);

	/**
	 * Finds the rdf:Class for this Java Class.
	 * 
	 * @param concept
	 * @return URI of the rdf:Class for this Java Class or null.
	 */
	public URI findType(Class<?> concept);

	/**
	 * Finds the rdf:types for concept and any sub-concept(s).
	 * 
	 * @param concept
	 * @param rdfTypes
	 * @return <code>rdfTypes</code>
	 */
	public abstract Collection<URI> findSubTypes(Class<?> role,
			Collection<URI> rdfTypes);

}