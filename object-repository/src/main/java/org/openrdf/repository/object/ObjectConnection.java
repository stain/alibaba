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

import static org.openrdf.query.QueryLanguage.SERQL;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.xml.namespace.QName;

import org.openrdf.elmo.EntitySupport;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.Mergeable;
import org.openrdf.elmo.Refreshable;
import org.openrdf.elmo.ResourceManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.sesame.SesameTransaction;
import org.openrdf.elmo.sesame.iterators.ConvertingIterator;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.elmo.sesame.roles.SesameManagerAware;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.exceptions.ElmoCompositionException;
import org.openrdf.repository.object.exceptions.ElmoIOException;
import org.openrdf.repository.object.exceptions.ElmoPersistException;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles operations of ElmoManager and EntityManager.
 * 
 * @author James Leigh
 * 
 */
public class ObjectConnection implements EntityManager {

	final Logger logger = LoggerFactory.getLogger(ObjectConnection.class);

	private ContextAwareConnection conn;

	private SesameTransaction trans;

	private Locale locale;

	private String language;

	private ResourceManager<Resource> resources;

	private LiteralManager<URI, Literal> lm;

	private RoleMapper<URI> mapper;

	private Map<Object, Resource> merged = new IdentityHashMap<Object, Resource>();

	public ContextAwareConnection getConnection() {
		if (!isOpen())
			throw new IllegalStateException("Connection has been closed");
		return conn;
	}

	public void setConnection(ContextAwareConnection connection) {
		this.conn = connection;
		this.trans = new SesameTransaction(conn);
	}

	public EntityTransaction getTransaction() {
		return trans;
	}

	public void joinTransaction() {
		if (!isOpen())
			throw new TransactionRequiredException();
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		if (locale != null) {
			String lang = locale.toString().replace('_', '-');
			language = lang.toLowerCase();
		}
	}

	public String getLanguage() {
		return language;
	}

	public ResourceManager<Resource> getResourceManager() {
		return resources;
	}

	public void setResourceManager(ResourceManager<Resource> manager) {
		this.resources = manager;
	}

	public LiteralManager<URI, Literal> getLiteralManager() {
		return lm;
	}

	public void setLiteralManager(LiteralManager<URI, Literal> manager) {
		this.lm = manager;
	}

	public RoleMapper<URI> getRoleMapper() {
		return mapper;
	}

	public void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	public boolean isOpen() {
		try {
			return conn != null && conn.isOpen();
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public void close() {
		try {
			if (conn != null && !trans.isActive())
				conn.close();
			conn = null;
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
	}

	public void close(Iterator<?> iter) {
		if (iter instanceof Closeable) {
			try {
				((Closeable) iter).close();
			} catch (IOException e) {
				throw new ElmoIOException(e);
			}
		}
	}

	public void flush() {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public FlushModeType getFlushMode() {
		return FlushModeType.AUTO;
	}

	public void setFlushMode(FlushModeType mode) {
		throw new UnsupportedOperationException();
	}

	public Object getDelegate() {
		return this;
	}

	public Object getInstance(Value value) {
		if (value instanceof Resource) {
			SesameEntity bean = this.find((Resource) value);
			if (logger.isDebugEnabled()) {
				try {
					if (!getConnection().hasMatch((Resource) value, null,
							null))
						logger.debug("Warning: Unknown entity: " + value);
				} catch (StoreException e) {
					throw new ElmoIOException(e);
				}
			}
			return bean;
		}
		return lm.getObject((Literal) value);
	}

	public Value getValue(Object instance) {
		if (instance instanceof SesameEntity)
			return ((SesameEntity) instance).getSesameResource();
		if (instance instanceof EntitySupport) {
			EntitySupport support = (EntitySupport) instance;
			RDFObject entity = support.getSupportedElmoEntity();
			if (entity instanceof SesameEntity)
				return ((SesameEntity) entity).getSesameResource();
		}
		Class<?> type = instance.getClass();
		if (lm.isTypeOfLiteral(type))
			return lm.getLiteral(instance);
		synchronized (merged) {
			if (merged.containsKey(instance))
				return merged.get(instance);
		}
		if (RDFObject.class.isAssignableFrom(type) || isEntity(type))
			return getValue(merge(instance));
		return lm.getLiteral(instance);
	}

	public Value getLocalizedValue(Object instance) {
		return lm.getLiteral(instance.toString(), language);
	}

	public boolean contains(Object entity) {
		if (entity instanceof SesameEntity) {
			SesameEntity se = (SesameEntity) entity;
			return this.equals(se.getSesameManager());
		} else if (entity instanceof EntitySupport) {
			EntitySupport es = (EntitySupport) entity;
			RDFObject e = es.getSupportedElmoEntity();
			if (e instanceof SesameEntity) {
				SesameEntity se = (SesameEntity) e;
				return this.equals(se.getSesameManager());
			}
		}
		return false;
	}

	public <T> T create(Class<T> concept, Class<?>... concepts) {
		Resource resource = resources.createResource(null);
		Class<?> proxy = resources.persistRole(resource, concept, concepts);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	public <T> T create(QName qname, Class<T> concept, Class<?>... concepts) {
		Resource resource = resources.createResource(qname);
		Class<?> proxy = resources.persistRole(resource, concept, concepts);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	@Deprecated
	public <T> T designate(Class<T> concept, Class<?>... concepts) {
		return create(concept, concepts);
	}

	public <T> T designate(QName qname, Class<T> concept, Class<?>... concepts) {
		Resource resource = resources.createResource(qname);
		return designate(resource, concept, concepts);
	}

	@Deprecated
	public <T> T designate(Class<T> concept, QName qname) {
		return designate(qname, concept);
	}

	public <T> T designate(Resource resource, Class<T> concept, Class<?>... concepts) {
		Class<?> proxy = resources.mergeRole(resource, concept, concepts);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	@Deprecated
	public <T> T designate(Class<T> concept, Resource resource) {
		return designate(resource, concept);
	}

	@Deprecated
	public <T> T designateEntity(Class<T> concept, Object entity) {
		return designateEntity(entity, concept);
	}

	public <T> T designateEntity(Object entity, Class<T> concept, Class<?>... concepts) {
		Resource resource = getSesameResource(entity);
		Class<?>[] roles = combine(concept, concepts);
		Class<?> proxy = resources.persistRole(resource, entity.getClass(), roles);
		RDFObject bean = createBean(resource, proxy);
		assert assertConceptsRecorded(bean, concepts);
		return (T) bean;
	}

	@Deprecated
	public RDFObject removeDesignation(Class<?> concept, Object entity) {
		return removeDesignation(entity, concept);
	}

	public RDFObject removeDesignation(Object entity, Class<?>... concepts) {
		Resource resource = getSesameResource(entity);
		return createBean(resource, resources.removeRole(resource, concepts));
	}

	public <T> T rename(T bean, QName qname) {
		Resource after = resources.createResource(qname);
		return rename(bean, after);
	}

	@SuppressWarnings("unchecked")
	public <T> T rename(T bean, Resource dest) {
		Resource before = getSesameResource(bean);
		resources.renameResource(before, dest);
		return (T) createBean(dest, resources.getEntityClass(dest));
	}

	public SesameEntity find(QName qname) {
		return find(resources.createResource(qname));
	}

	@Deprecated
	public QName asQName(Resource resource) {
		return getResourceManager().createQName(resource);
	}

	public SesameEntity find(Resource resource) {
		return createBean(resource, resources.getEntityClass(resource));
	}

	public <T> T find(Class<T> concept, Object qname) {
		assert qname instanceof QName : qname;
		SesameEntity entity = find((QName) qname);
		if (concept.isInstance(entity))
			return concept.cast(entity);
		return null;
	}

	public <T> T getReference(Class<T> concept, Object qname) {
		return find(concept, qname);
	}

	public void refresh(Object entity) {
		if (entity instanceof Refreshable) {
			((Refreshable) entity).refresh();
		}
	}

	public <T> T merge(T bean) {
		if (bean == null) {
			return null;
		} else if (bean instanceof Set<?>) {
			// so we can merge both a List and a Set
			Set<?> old = (Set<?>) bean;
			Set<Object> set = new HashSet<Object>(old.size());
			for (Object o : old) {
				set.add(merge(o));
			}
			return (T) set;
		} else {
			Resource resource = assignResource(bean);
			Class<?> role = bean.getClass();
			Class<?> proxy;
			if (resource instanceof URI) {
				proxy = resources.mergeRole(resource, role);
			} else {
				proxy = resources.persistRole(resource, role);
			}
			SesameEntity result = createBean(resource, proxy);
			assert result instanceof Mergeable;
			((Mergeable) result).merge(bean);
			return (T) result;
		}
	}

	public void persist(Object bean) {
		Resource resource = assignResource(bean);
		Class<?> role = bean.getClass();
		Class<?> proxy = resources.persistRole(resource, role);
		SesameEntity result = createBean(resource, proxy);
		assert result instanceof Mergeable;
		((Mergeable) result).merge(bean);
	}

	public ObjectQuery createQuery(String query) {
		try {
			TupleQuery qry = getConnection().prepareTupleQuery(query);
			return new ObjectQuery(this, qry);
		} catch (MalformedQueryException e) {
			throw new ElmoIOException(e);
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public ObjectQuery createNativeQuery(String serql) {
		try {
			TupleQuery qry = getConnection()
					.prepareTupleQuery(SERQL, serql, "");
			return new ObjectQuery(this, qry);
		} catch (MalformedQueryException e) {
			throw new ElmoIOException(e);
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public ObjectQuery createNativeQuery(String serql, Class concept) {
		try {
			TupleQuery qry = getConnection()
					.prepareTupleQuery(SERQL, serql, "");
			return new ObjectQuery(this, qry);
		} catch (MalformedQueryException e) {
			throw new ElmoIOException(e);
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public ObjectQuery createNativeQuery(String serql, String nil) {
		try {
			TupleQuery qry = getConnection()
					.prepareTupleQuery(SERQL, serql, "");
			return new ObjectQuery(this, qry);
		} catch (MalformedQueryException e) {
			throw new ElmoIOException(e);
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public Query createNamedQuery(String name) {
		throw new UnsupportedOperationException(
				"Named queries are not supported");
	}

	public <T> Iterable<T> findAll(final Class<T> javaClass) {
		final ResourceManager<Resource> resources = this.resources;
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				Iterator<Resource> iter = resources.createRoleQuery(javaClass);
				return new ConvertingIterator<Resource, T>(iter) {
					@Override
					public T convert(Resource resource) {
						return (T) find(resource);
					}
				};
			}
		};
	}

	public void remove(Object entity) {
		Resource resource = getSesameResource(entity);
		resources.removeResource(resource);
	}

	private boolean isEntity(Class<?> type) {
		if (type == null)
			return false;
		for (Class<?> face : type.getInterfaces()) {
			if (mapper.findType(face) != null)
				return true;
		}
		if (mapper.findType(type) != null)
			return true;
		return isEntity(type.getSuperclass());
	}

	private boolean assertConceptsRecorded(RDFObject bean, Class<?>... concepts) {
		for (Class<?> concept : concepts) {
			assert !concept.isInterface()
					|| concept.isAssignableFrom(bean.getClass()) : "Concept has not bean recorded: "
					+ concept.getSimpleName();
		}
		return true;
	}

	private Resource assignResource(Object bean) {
		synchronized (merged) {
			if (merged.containsKey(bean))
				return merged.get(bean);
			Resource resource = findResource(bean);
			if (resource == null)
				resource = resources.createResource(null);
			merged.put(bean, resource);
			return resource;
		}
	}

	private Resource getSesameResource(Object entity) {
		Resource resource = getResource(entity);
		if (resource == null)
			throw new ElmoPersistException("Unknown Entity: " + entity);
		return resource;
	}

	private Resource findResource(Object bean) {
		Resource resource = getResource(bean);
		if (resource != null)
			return resource;
		if (bean instanceof RDFObject) {
			QName name = ((RDFObject) bean).getQName();
			if (name == null)
				return null;
			return resources.createResource(name);
		} else {
			try {
				Method m = bean.getClass().getMethod("getQName");
				QName name = (QName) m.invoke(bean);
				if (name == null)
					return null;
				return resources.createResource(name);
			} catch (Exception e) {
				return null;
			}
		}
	}

	private Resource getResource(Object bean) {
		if (bean instanceof SesameEntity) {
			return ((SesameEntity) bean).getSesameResource();
		} else if (bean instanceof EntitySupport) {
			EntitySupport support = (EntitySupport) bean;
			RDFObject entity = support.getSupportedElmoEntity();
			if (entity instanceof SesameEntity)
				return ((SesameEntity) entity).getSesameResource();
		}
		return null;
	}

	private SesameEntity createBean(Resource resource, Class<?> type) {
		try {
			Object obj = type.newInstance();
			assert obj instanceof SesameManagerAware : "core roles are not registered, check your deployed classpath";
			SesameManagerAware bean = (SesameManagerAware) obj;
			bean.initSesameManager(this);
			bean.initSesameResource(resource);
			return bean;
		} catch (InstantiationException e) {
			throw new ElmoCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ElmoCompositionException(e);
		}
	}

	private <T> Class<?>[] combine(Class<T> concept, Class<?>... concepts) {
		Class<?>[] roles;
		if (concepts == null || concepts.length == 0) {
			roles = new Class<?>[]{concept};
		} else {
			roles = new Class<?>[concepts.length + 1];
			roles[0] = concept;
			System.arraycopy(concepts, 0, roles, 1, concepts.length);
		}
		return roles;
	}

	public void lock(Object entity, LockModeType mode) {
		throw new UnsupportedOperationException();
	}

}
