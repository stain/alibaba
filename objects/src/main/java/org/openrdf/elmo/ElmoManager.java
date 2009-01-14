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
import java.util.Locale;

import javax.persistence.EntityTransaction;
import javax.xml.namespace.QName;

/**
 * Manages the basic operations of Elmo JavaBeans.
 * 
 * @author James Leigh
 * 
 */
public interface ElmoManager {

	/**
	 * Locale this manager was created with.
	 * 
	 * @return Locale or null
	 */
	public abstract Locale getLocale();

	/**
	 * If this manager currently has an open connection to the repository.
	 * 
	 * @return <code>true</code> if the connection is open.
	 */
	public abstract boolean isOpen();

	/**
	 * Closes any transactions or connections in the manager.
	 * 
	 */
	public abstract void close();

	/**
	 * Close an Iterator created by iterate() immediately, instead of waiting
	 * until the iteration is complete or connection is closed.
	 * 
	 */
	public abstract void close(Iterator<?> iter);

	/**
	 * Returns the resource-level transaction object. The EntityTransaction
	 * instance may be used serially to begin and commit multiple transactions.
	 * 
	 * @return EntityTransaction instance
	 */
	public EntityTransaction getTransaction();

	/**
	 * Check if the instance belongs to the current persistence context.
	 * 
	 * @param entity
	 * @return <code>true</code> if the instance belongs to the current
	 *         persistence context.
	 */
	public abstract boolean contains(Object entity);

	/**
	 * Assigns <code>type</code> to a new anonymous entity.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing the subject.
	 */
	public abstract <T> T create(Class<T> concept, Class<?>... concepts);

	/**
	 * Assigns <code>concept</code> to the new named entity.
	 * 
	 * @param qname
	 *            URI of the new entity that does not exist in the repository.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * 
	 * @return Java Bean representing the subject.
	 */
	public abstract <T> T create(QName qname, Class<T> concept,
			Class<?>... concepts);

	/**
	 * Assigns <code>type</code> to a new anonymous entity. Use create instead.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing the subject.
	 */
	@Deprecated
	public abstract <T> T designate(Class<T> concept, Class<?>... concepts);

	/**
	 * Assigns <code>concept</code> to the named entity subject.
	 * 
	 * @param qname
	 *            URI of the entity that may exist in the repository.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * 
	 * @return Java Bean representing the subject.
	 */
	public abstract <T> T designate(QName qname, Class<T> concept,
			Class<?>... concepts);

	/**
	 * Assigns <code>concept</code> to the named entity subject.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param qname
	 *            URI of the entity.
	 * 
	 * @return Java Bean representing the subject.
	 */
	@Deprecated
	public abstract <T> T designate(Class<T> concept, QName qname);

	/**
	 * Assigns <code>concept</code> to the given entity and return a new object
	 * reference that implements the given <code>concept</code>.
	 * 
	 * @param <T>
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param concepts
	 *            additional interfaces to be translated to rdf:type.
	 * @return Java Bean representing <code>entity</code> that implements
	 *         <code>concept</code>.
	 */
	public abstract <T> T designateEntity(Object entity, Class<T> concept,
			Class<?>... concepts);

	/**
	 * Assigns <code>concept</code> to the given entity and return a new object
	 * reference that implements the given <code>concept</code>.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * 
	 * @param <T>
	 * @return Java Bean representing <code>entity</code> that implements
	 *         <code>concept</code>.
	 */
	@Deprecated
	public abstract <T> T designateEntity(Class<T> concept, Object entity);

	/**
	 * Removes the <code>concept</code> designation from this
	 * <code>entity</code>.
	 * 
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * @param concepts
	 *            interface to be translated to rdf:type.
	 * @return Java Bean representing <code>entity</code> that does not
	 *         implement <code>concept</code>.
	 */
	public abstract Entity removeDesignation(Object entity,
			Class<?>... concepts);

	/**
	 * Removes the <code>concept</code> designation from this
	 * <code>entity</code>.
	 * 
	 * @param concept
	 *            interface to be translated to rdf:type.
	 * @param entity
	 *            An existing entity retrieved from this manager.
	 * 
	 * @return Java Bean representing <code>entity</code> that does not
	 *         implement <code>concept</code>.
	 */
	@Deprecated
	public abstract Entity removeDesignation(Class<?> concept, Object entity);

	/**
	 * Removes all the references to the given <code>entity</code> and replaces
	 * them with references to the new <code>qname</code>. It is the
	 * responsibility of the caller to ensure that any object references to this
	 * resource are replaced with the returned object. Previous referenced
	 * objects must no longer be used and any cached values must be refreshed.
	 * 
	 * @param entity
	 *            current Entity to be renamed
	 * @param qname
	 *            new qualified name of the entity
	 * @return <code>entity</code> with the given <code>qname</code>.
	 */
	public abstract <T> T rename(T entity, QName qname);

	/**
	 * Retrieves the rdf:type, creates a Java Bean class and instantiates it.
	 * 
	 * @param qname
	 *            URI of the entity.
	 * @return JavaBean representing the subject.
	 */
	public abstract Entity find(QName qname);

	/**
	 * Creates an ElmoQuery to evaluate the query string.
	 * 
	 * @param query
	 *            rdf query in the configured language - default SPARQL.
	 * @return {@link ElmoQuery}.
	 */
	public abstract ElmoQuery createQuery(String query);

	/**
	 * Creates an iteration of entities that implement this <code>role</code>.
	 * 
	 * @param role
	 *            concept or behaviour to be translated to one or more
	 *            rdf:types.
	 * @return Iterable entities that implement role.
	 */
	public abstract <T> Iterable<T> findAll(Class<T> role);

	/**
	 * If <code>entity</code> implements Refreshable, its method
	 * {@link Refreshable#refresh()} will be called. This call instructs
	 * entities that their property values may have changed and they should
	 * reload them as needed.
	 * 
	 * @param entity
	 */
	public abstract void refresh(Object entity);

	/**
	 * Copies all non-null values from bean into an entity managed by this
	 * manager. If <code>bean</code> implements {@link Entity} its QName will be
	 * used to look up the managed entity, otherwise a new anonymous entity will
	 * be created.
	 * 
	 * @param <T>
	 * @param bean
	 *            with values that shoud be merged
	 * @return managed entity it was merged with
	 */
	public abstract <T> T merge(T bean);

	/**
	 * Copies all non-null values from bean into an entity managed by this
	 * manager. If <code>bean</code> implements {@link Entity} its QName will be
	 * used to look up the managed entity, otherwise a new anonymous entity will
	 * be created.
	 * 
	 * @param bean
	 *            with values that shoud be merged
	 */
	public abstract void persist(Object bean);

	/**
	 * Removes the given entity or subject and all implementing roles. It is the
	 * responsibility of the caller to ensure this <code>entity</code> or any
	 * other object referencing it are no longer used and any object that may
	 * have cached a value containing this is refreshed.
	 * 
	 * @param entity
	 *            to be removed from the pool and repository.
	 */
	public abstract void remove(Object entity);

}
