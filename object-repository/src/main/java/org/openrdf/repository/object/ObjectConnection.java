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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.repository.object.result.ObjectIterator;
import org.openrdf.repository.object.traits.Mergeable;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectConnection extends ContextAwareConnection {

	final Logger logger = LoggerFactory.getLogger(ObjectConnection.class);

	private String language;

	private TypeManager types;

	private ObjectFactory factory;

	private Map<Object, Resource> merged = new IdentityHashMap<Object, Resource>();

	public ObjectConnection(ObjectRepository repository,
			RepositoryConnection connection, ObjectFactory factory,
			TypeManager types) throws StoreException {
		super(repository, connection);
		this.factory = factory;
		this.types = types;
		types.setConnection(this);
		factory.setObjectConnection(this);
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String lang) {
		this.language = lang;
	}

	public ObjectFactory getObjectFactory() {
		return factory;
	}

	public void close(Iterator<?> iter) {
		ObjectIterator.close(iter);
	}

	public Object getObject(Value value) throws StoreException {
		if (value instanceof Literal)
			return factory.createObject((Literal) value);
		Resource resource = (Resource) value;
		return factory.createRDFObject(resource, types.getTypes(resource));
	}

	public <T> T addType(Object entity, Class<T> concept) throws StoreException {
		Resource resource = findResource(entity);
		Collection<URI> types = new ArrayList<URI>();
		getTypes(entity.getClass(), types);
		addConcept(resource, concept, types);
		Object bean = factory.createRDFObject(resource, types);
		assert assertConceptRecorded(bean, concept);
		return (T) bean;
	}

	public Object removeType(Object entity, Class<?> concept)
			throws StoreException {
		Resource resource = findResource(entity);
		Collection<URI> types = new ArrayList<URI>();
		getTypes(entity.getClass(), types);
		removeConcept(resource, concept, types);
		return factory.createRDFObject(resource, types);
	}

	public Value addObject(Object instance) throws StoreException {
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
			if (factory.isDatatype(instance.getClass()))
				return factory.createLiteral(instance);
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
		return factory.createLiteral(instance);
	}

	public void addObject(Resource resource, Object instance)
			throws StoreException {
		if (instance instanceof RDFObjectBehaviour) {
			RDFObjectBehaviour support = (RDFObjectBehaviour) instance;
			Object entity = support.getBehaviourDelegate();
			if (entity != instance) {
				addObject(resource, entity);
				return;
			}
		}
		Class<?> entity = instance.getClass();
		List<URI> list = getTypes(entity, new ArrayList<URI>());
		for (URI type : list) {
			types.addTypeStatement(resource, type);
		}
		Object result = factory.createRDFObject(resource, list);
		((Mergeable) result).merge(instance);
	}

	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query,
			String baseURI) throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query, baseURI));
	}

	public ObjectQuery prepareObjectQuery(QueryLanguage ql, String query)
			throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(ql, query));
	}

	public ObjectQuery prepareObjectQuery(String query)
			throws MalformedQueryException, StoreException {
		return new ObjectQuery(this, prepareTupleQuery(query));
	}


	private Resource findResource(Object object) {
		if (object instanceof RDFObject)
			return ((RDFObject) object).getResource();
		if (object instanceof RDFObjectBehaviour) {
			RDFObjectBehaviour support = (RDFObjectBehaviour) object;
			Object entity = support.getBehaviourDelegate();
			if (entity instanceof RDFObject)
				return ((RDFObject) entity).getResource();
		}
		throw new ObjectPersistException(
				"Object not created by this ObjectFactory: "
						+ object.getClass().getSimpleName());
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		for (Class<?> face : type.getInterfaces()) {
			if (factory.isConcept(face))
				return true;
		}
		if (factory.isConcept(type))
			return true;
		return isEntity(type.getSuperclass());
	}

	private boolean assertConceptRecorded(Object bean, Class<?> concept) {
		assert !concept.isInterface()
				|| concept.isAssignableFrom(bean.getClass()) : "Concept has not bean recorded: "
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
			throws StoreException {
		URI type = factory.getType(role);
		if (type != null) {
			set.add(type);
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			for (Class<?> of : role.getAnnotation(intersectionOf.class).value()) {
				getTypes(of, set);
			}
		} else {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				getTypes(superclass, set);
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				getTypes(interfaces[i], set);
			}
		}
		return set;
	}

	private <C extends Collection<URI>> C addConcept(Resource resource,
			Class<?> role, C set) throws StoreException {
		URI type = factory.getType(role);
		if (type != null) {
			types.addTypeStatement(resource, type);
			set.add(type);
		} else if (role.isAnnotationPresent(complementOf.class)) {
			removeConcept(resource, role.getAnnotation(complementOf.class)
					.value(), set);
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			for (Class<?> of : role.getAnnotation(intersectionOf.class).value()) {
				addConcept(resource, of, set);
			}
		} else {
			throw new ObjectPersistException("Concept not registered: "
					+ role.getSimpleName());
		}
		return set;
	}

	private <C extends Collection<URI>> C removeConcept(Resource resource,
			Class<?> role, C set) throws StoreException {
		URI type = factory.getType(role);
		if (type != null) {
			types.removeTypeStatement(resource, type);
			set.remove(type);
		} else if (role.isAnnotationPresent(complementOf.class)) {
			Class<?> values = role.getAnnotation(complementOf.class).value();
			addConcept(resource, values, set);
		} else if (role.isAnnotationPresent(intersectionOf.class)) {
			throw new ObjectPersistException("Cannot remove intersections: "
					+ role.getSimpleName());
		} else {
			throw new ObjectPersistException("Concept not registered: "
					+ role.getSimpleName());
		}
		return set;
	}

}
