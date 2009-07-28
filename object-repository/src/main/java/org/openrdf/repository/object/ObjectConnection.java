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
package org.openrdf.repository.object;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.LookAheadIteration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.annotations.localized;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.result.ObjectIterator;
import org.openrdf.repository.object.traits.Mergeable;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.result.Result;
import org.openrdf.result.impl.ResultImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary interface for object retrieval and persistence.
 * 
 * @author James Leigh
 *
 */
public class ObjectConnection extends ContextAwareConnection {
	final Logger logger = LoggerFactory.getLogger(ObjectConnection.class);
	private ObjectRepository repository;
	private String language;
	private TypeManager types;
	private ObjectFactory of;
	private Map<Object, Resource> merged = new IdentityHashMap<Object, Resource>();
	private Map<Class, Map<Integer, ObjectQuery>> queries = new HashMap<Class, Map<Integer, ObjectQuery>>();

	public ObjectConnection(ObjectRepository repository,
			RepositoryConnection connection, ObjectFactory factory,
			TypeManager types) throws RepositoryException {
		super(repository, connection);
		this.repository = repository;
		this.of = factory;
		this.types = types;
		types.setConnection(this);
		factory.setObjectConnection(this);
	}

	@Override
	public ObjectRepository getRepository() {
		return repository;
	}

	@Override
	public void close() throws RepositoryException {
		super.close();
		repository.closed(this);
	}

	/**
	 * The assign language for this connection, if any.
	 * 
	 * @return language tag ("en") or null
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Assigns a language to this connection.
	 * @param lang such as "en"
	 * @see localized
	 */
	public void setLanguage(String lang) {
		this.language = lang;
	}

	/**
	 * Access to the ObjectFactory used with this connection.
	 * 
	 * @return ObjectFactory bound to this connection.
	 */
	public ObjectFactory getObjectFactory() {
		return of;
	}

	/**
	 * Closes open iterators.
	 * 
	 * @param iter
	 */
	public void close(Iterator<?> iter) {
		ObjectIterator.close(iter);
	}

	/**
	 * Imports the instance into the RDF store, returning its RDF handle.
	 */
	public Value addObject(Object instance) throws RepositoryException {
		if (instance instanceof RDFObjectBehaviour) {
			RDFObjectBehaviour support = (RDFObjectBehaviour) instance;
			Object entity = support.getBehaviourDelegate();
			if (entity != instance)
				return addObject(entity);
		}
		if (instance instanceof RDFObject) {
			if (((RDFObject) instance).getObjectConnection() == this)
				return ((RDFObject) instance).getResource();
		} else {
			if (of.isDatatype(instance.getClass()))
				return of.createLiteral(instance);
		}
		Class<?> type = instance.getClass();
		if (RDFObject.class.isAssignableFrom(type) || isEntity(type)) {
			synchronized (merged) {
				if (merged.containsKey(instance))
					return merged.get(instance);
			}
			Resource resource = assignResource(instance);
			addObject(resource, instance);
			return resource;
		}
		return of.createLiteral(instance);
	}

	/**
	 * Imports the entity into the RDF store using the given handle.
	 */
	public void addObject(Resource resource, Object entity)
			throws RepositoryException {
		if (entity instanceof RDFObjectBehaviour) {
			RDFObjectBehaviour support = (RDFObjectBehaviour) entity;
			Object delegate = support.getBehaviourDelegate();
			if (delegate != entity) {
				addObject(resource, delegate);
				return;
			}
		}
		boolean autoCommit = isAutoCommit();
		if (autoCommit) {
			setAutoCommit(false);
		}
		try {
			Class<?> proxy = entity.getClass();
			List<URI> list = getTypes(proxy, new ArrayList<URI>());
			for (URI type : list) {
				types.addTypeStatement(resource, type);
			}
			Object result = of.createObject(resource, list);
			if (result instanceof Mergeable) {
				((Mergeable) result).merge(entity);
			}
			if (autoCommit) {
				setAutoCommit(true);
			}
		} finally {
			if (autoCommit && !isAutoCommit()) {
				rollback();
				setAutoCommit(true);
			}
		}
	}

	/**
	 * Explicitly adds the concept to the entity.
	 * 
	 * @return the entity with new composed concept
	 */
	public <T> T addDesignation(Object entity, Class<T> concept) throws RepositoryException {
		Resource resource = findResource(entity);
		Collection<URI> types = new ArrayList<URI>();
		getTypes(entity.getClass(), types);
		addConcept(resource, concept, types);
		Object bean = of.createObject(resource, types);
		assert assertConceptRecorded(bean, concept);
		return (T) bean;
	}

	/**
	 * Explicitly adds the types to the entity.
	 * 
	 * @return the entity with new composed types
	 */
	public Object addDesignations(Object entity, URI... types) throws RepositoryException {
		assert types != null && types.length > 0;
		Resource resource = findResource(entity);
		Collection<URI> list = new ArrayList<URI>();
		getTypes(entity.getClass(), list);
		for (URI type : types) {
			this.types.addTypeStatement(resource, type);
			list.add(type);
		}
		return of.createObject(resource, list);
	}

	/**
	 * Explicitly removes the concept from the entity.
	 */
	public void removeDesignation(Object entity, Class<?> concept)
			throws RepositoryException {
		Resource resource = findResource(entity);
		URI type = of.getType(concept);
		if (type == null) {
			throw new ObjectPersistException(
					"Concept is anonymous or is not registered: "
							+ concept.getSimpleName());
		}
		types.removeTypeStatement(resource, type);
	}

	/**
	 * Explicitly removes the types from the entity.
	 */
	public void removeDesignations(Object entity, URI... types) throws RepositoryException {
		assert types != null && types.length > 0;
		Resource resource = findResource(entity);
		for (URI type : types) {
			this.types.removeTypeStatement(resource, type);
		}
	}

	/**
	 * Loads a single Object by URI in String form.
	 */
	public Object getObject(String uri) throws RepositoryException {
		assert uri != null;
		return getObject(getValueFactory().createURI(uri));
	}

	/**
	 * Loads a single Object or converts the literal into an Object.
	 */
	public Object getObject(Value value) throws RepositoryException {
		assert value != null;
		if (value instanceof Literal)
			return of.createObject((Literal) value);
		Resource resource = (Resource) value;
		return of.createObject(resource, types.getTypes(resource));
	}

	/**
	 * Loads a single Object that is assumed to be of the given concept.
	 */
	public <T> T getObject(Class<T> concept, Resource resource)
			throws RepositoryException, QueryEvaluationException {
		return getObjects(concept, resource).singleResult();
	}

	/**
	 * Matches objects that have the given concept rdf:type. This method will
	 * include all objects that implement the given concept or a subclass of the
	 * concept. The concept must be a named concept and cannot be mapped to
	 * rdfs:Resource. The result of this method is not guaranteed to be unique
	 * and may continue duplicates. Use the {@link Result#asSet()} method to
	 * ensure uniqueness.
	 * 
	 * @see #addDesignation(Object, Class)
	 */
	public synchronized <T> Result<T> getObjects(Class<T> concept)
			throws RepositoryException,
			QueryEvaluationException {
		try {
			return getObjectQuery(concept, 0).evaluate(concept);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Loads the list of resources assumed to implement the given concept. The
	 * concept must be a named concept and cannot be mapped to rdfs:Resource.
	 */
	public synchronized <T> Result<T> getObjects(final Class<T> concept,
			Resource... resources) throws RepositoryException,
			QueryEvaluationException {
		try {
			int size = resources.length;
			ObjectQuery query = getObjectQuery(concept, size);
			for (int i = 0; i < size; i++) {
				query.setBinding("_" + i, resources[i]);
			}
			final List<Resource> list = new ArrayList<Resource>(size);
			list.addAll(Arrays.asList(resources));
			CloseableIteration<T, QueryEvaluationException> iter;
			final Result<T> result = query.evaluate(concept);
			iter = new LookAheadIteration<T, QueryEvaluationException>() {
				@Override
				protected T getNextElement() throws QueryEvaluationException {
					T next = result.next();
					if (next != null) {
						list.remove(((RDFObject) next).getResource());
						return next;
					}
					if (!list.isEmpty())
						return (T) of.createObject(list.remove(0));
					return null;
				}
			};
			return new ResultImpl<T>(iter);
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Creates a new query that returns object(s).
	 */
	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query,
			String baseURI) throws MalformedQueryException, RepositoryException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query, baseURI));
	}

	/**
	 * Creates a new query that returns object(s).
	 */
	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, RepositoryException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query));
	}

	/**
	 * Creates a new query that returns object(s).
	 */
	public ObjectQuery prepareObjectQuery(String query)
			throws MalformedQueryException, RepositoryException {
		return new ObjectQuery(this, prepareTupleQuery(query));
	}

	/** method and result synchronised on this */
	private <T> ObjectQuery getObjectQuery(Class<T> concept,
			int length) throws MalformedQueryException,
			RepositoryException {
		if (queries.containsKey(concept)
				&& queries.get(concept).containsKey(length)) {
			return queries.get(concept).get(length);
		} else {
			String sparql = of.createObjectQuery(concept, length);
			ObjectQuery query = prepareObjectQuery(SPARQL, sparql);
			Map<Integer, ObjectQuery> map = queries.get(concept);
			if (map == null) {
				queries.put(concept, map = new HashMap<Integer, ObjectQuery>());
			}
			map.put(length, query);
			return query;
		}
	}

	private Resource findResource(Object object) {
		if (object instanceof RDFObject)
			return ((RDFObject) object).getResource();
		throw new ObjectPersistException(
				"Object not created by this ObjectFactory: "
						+ object.getClass().getSimpleName());
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		for (Class<?> face : type.getInterfaces()) {
			if (of.isNamedConcept(face))
				return true;
		}
		if (of.isNamedConcept(type))
			return true;
		return isEntity(type.getSuperclass());
	}

	private boolean assertConceptRecorded(Object bean, Class<?> concept) {
		assert !concept.isInterface()
				|| concept.isAssignableFrom(bean.getClass()) : "Concept is Anonymous or has not bean recorded: "
				+ concept.getSimpleName();
		return true;
	}

	private Resource assignResource(Object bean) {
		synchronized (merged) {
			if (merged.containsKey(bean))
				return merged.get(bean);
			Resource resource = null;
			if (bean instanceof RDFObject) {
				resource = ((RDFObject) bean).getResource();
			}
			if (resource == null) {
				resource = getValueFactory().createBNode();
			}
			merged.put(bean, resource);
			return resource;
		}
	}

	private <C extends Collection<URI>> C getTypes(Class<?> role, C set)
			throws RepositoryException {
		URI type = of.getType(role);
		if (type == null) {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				getTypes(superclass, set);
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				getTypes(interfaces[i], set);
			}
		} else {
			set.add(type);
		}
		return set;
	}

	private <C extends Collection<URI>> C addConcept(Resource resource,
			Class<?> role, C set) throws RepositoryException {
		URI type = of.getType(role);
		if (type == null) {
			throw new ObjectPersistException(
					"Concept is anonymous or is not registered: "
							+ role.getSimpleName());
		}
		types.addTypeStatement(resource, type);
		set.add(type);
		return set;
	}

}
