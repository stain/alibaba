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
package org.openrdf.elmo.sesame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.elmo.ElmoProperty;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.elmo.sesame.iterators.ElmoIteration;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.ElmoIOException;
import org.openrdf.repository.object.exceptions.ElmoPersistException;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

/**
 * A set for a given getResource(), predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 */
public class SesameProperty<E> implements ElmoProperty<E>, Set<E> {
	private static final int CACHE_LIMIT = 10;
	private final SesameEntity bean;
	protected PropertyChanger property;
	List<E> cache;
	boolean cached;

	public SesameProperty(SesameEntity bean, PropertyChanger property) {
		assert bean != null;
		assert property != null;
		this.bean = bean;
		this.property = property;
	}

	public void refresh() {
		cached = false;
		cache = null;
	}

	/**
	 * This method always returns <code>true</code>
	 * @return <code>true</code>
	 */
	public boolean add(E o) {
		ContextAwareConnection conn = getConnection();
		try {
			add(conn, getResource(), getValue(o));
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshEntity();
		refresh(o);
		return true;
	}

	public boolean addAll(Collection<? extends E> c) {
		RepositoryConnection conn = getConnection();
		boolean modified = false;
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			for (E o : c)
				if (add(o))
					modified = true;
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshEntity();
		return modified;
	}

	public void clear() {
		try {
			property.remove(getConnection(), getResource(), null);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshCache();
		refreshEntity();
	}

	public boolean contains(Object o) {
		if (isCacheComplete())
			return cache.contains(o);
		if (cached && cache.contains(o))
			return true;
		Value val = getValue(o);
		ContextAwareConnection conn = getConnection();
		try {
			return conn.hasMatch(getResource(), getURI(), val);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
	}

	public boolean containsAll(Collection<?> c) {
		if (isCacheComplete())
			return cache.containsAll(c);
		if (cached && cache.containsAll(c))
			return true;
		Iterator<?> e = c.iterator();
		while (e.hasNext())
			if (!contains(e.next()))
				return false;
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!o.getClass().equals(this.getClass()))
			return false;
		SesameProperty<?> p = (SesameProperty<?>) o;
		if (!getResource().equals(p.getResource()))
			return false;
		if (!property.equals(p.property))
			return false;
		if (!getManager().equals(p.getManager()))
			return false;
		return true;
	}

	public Set<E> getAll() {
		return this;
	}

	public E getSingle() {
		if (cached && cache.isEmpty())
			return null;
		if (cached)
			return cache.get(0);
		ElmoIteration<Statement, E> iter = getElmoIteration();
		try {
			if (iter.hasNext())
				return iter.next();
			return null;
		} finally {
			iter.close();
		}
	}

	@Override
	public int hashCode() {
		int hashCode = getResource().hashCode();
		hashCode ^= property.hashCode();
		return hashCode;
	}

	public boolean isEmpty() {
		if (cached)
			return cache.isEmpty();
		ElmoIteration<Statement, E> iter = getElmoIteration();
		try {
			return !iter.hasNext();
		} finally {
			iter.close();
		}
	}

	/**
	 * This method always returns <code>true</code>
	 * @return <code>true</code>
	 */
	public boolean remove(Object o) {
		ContextAwareConnection conn = getConnection();
		try {
			remove(conn, getResource(), getValue(o));
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refresh(o);
		refreshEntity();
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		RepositoryConnection conn = getConnection();
		boolean modified = false;
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			for (Object o : c)
				if (remove(o))
					modified = true;
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshCache();
		refreshEntity();
		return modified;
	}

	public boolean retainAll(Collection<?> c) {
		RepositoryConnection conn = getConnection();
		boolean modified = false;
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			ElmoIteration<Statement, E> e = getElmoIteration();
			try {
				while (e.hasNext()) {
					if (!c.contains(e.next())) {
						e.remove();
						modified = true;
					}
				}
			} finally {
				e.close();
			}
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshCache();
		refreshEntity();
		return modified;
	}

	public void setAll(Set<E> set) {
		if (this == set)
			return;
		if (set == null) {
			clear();
			return;
		}
		Set<E> c = new HashSet<E>(set);
		RepositoryConnection conn = getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			if (!cached || !cache.isEmpty()) {
				clear();
			}
			addAll(c);
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
		refreshCache();
	}

	public void setSingle(E o) {
		if (o == null) {
			clear();
		} else {
			RepositoryConnection conn = getConnection();
			try {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit)
					conn.setAutoCommit(false);
				if (!cached || !cache.isEmpty()) {
					clear();
				}
				add(o);
				if (autoCommit)
					conn.setAutoCommit(true);
			} catch (StoreException e) {
				throw new ElmoPersistException(e);
			}
		}
	}

	public int size() {
		if (isCacheComplete())
			return cache.size();
		ModelResult iter;
		try {
			iter = getStatements();
			try {
				int size;
				for (size = 0; iter.hasNext(); size++)
					iter.next();
				return size;
			} finally {
				iter.close();
			}
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

	public Iterator<E> iterator() {
		if (isCacheComplete()) {
			final Iterator<E> iter = cache.iterator();
			return new Iterator<E>() {
				private E e;

				public boolean hasNext() {
					return iter.hasNext();
				}

				public E next() {
					return e = iter.next();
				}

				public void remove() {
					SesameProperty.this.remove(e);
				}
			};
		}
		return getElmoIteration();
	}

	public Object[] toArray() {
		if (isCacheComplete())
			return cache.toArray();
		List<E> list = new ArrayList<E>();
		ElmoIteration<Statement, E> iter = getElmoIteration();
		try {
			while (iter.hasNext()) {
				list.add(iter.next());
			}
		} finally {
			iter.close();
		}
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		if (isCacheComplete())
			return cache.toArray(a);
		List<E> list = new ArrayList<E>();
		ElmoIteration<Statement, E> iter = getElmoIteration();
		try {
			while (iter.hasNext()) {
				list.add(iter.next());
			}
		} finally {
			iter.close();
		}
		return list.toArray(a);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		ElmoIteration<Statement, E> iter = getElmoIteration();
		try {
			if (iter.hasNext()) {
				sb.append(iter.next().toString());
			}
			while (iter.hasNext()) {
				sb.append(", ");
				sb.append(iter.next());
			}
		} finally {
			iter.close();
		}
		return sb.toString();
	}

	final ObjectConnection getManager() {
		return bean.getSesameManager();
	}

	final ContextAwareConnection getConnection() {
		return getManager().getConnection();
	}

	final Resource getResource() {
		return bean.getSesameResource();
	}

	final URI getURI() {
		return property.getPredicate();
	}

	void add(ContextAwareConnection conn, Resource subj, Value obj)
			throws StoreException {
		property.add(conn, subj, obj);
	}

	void remove(ContextAwareConnection conn, Resource subj, Value obj)
			throws StoreException {
		property.remove(conn, subj, obj);
	}

	void remove(ContextAwareConnection conn, Statement stmt)
			throws StoreException {
		assert stmt.getPredicate().equals(getURI());
		remove(conn, stmt.getSubject(), stmt.getObject());
	}

	@SuppressWarnings("unchecked")
	E createInstance(Statement stmt) {
		Value value = stmt.getObject();
		return (E) getManager().getInstance(value);
	}

	ModelResult getStatements()
			throws StoreException {
		ContextAwareConnection conn = getConnection();
		return conn.match(getResource(), getURI(), null);
	}

	Value getValue(Object instance) {
		if (instance instanceof Value)
			return (Value) instance;
		return getManager().getValue(instance);
	}

	protected void refreshCache() {
		if (cached) {
			for (E e : cache) {
				refresh(e);
			}
		}
	}

	protected void refresh(Object o) {
		getManager().refresh(o);
	}

	protected void refreshEntity() {
		refresh();
		bean.refresh();
	}

	private boolean isCacheComplete() {
		return cached && cache.size() < CACHE_LIMIT;
	}

	private ElmoIteration<Statement, E> getElmoIteration() {
		try {
			return new ElmoIteration<Statement, E>(getStatements()) {
				private List<E> list = new ArrayList<E>(CACHE_LIMIT);

				@Override
				protected E convert(Statement stmt) {
					E instance = createInstance(stmt);
					if (list != null && list.size() < CACHE_LIMIT)
						list.add(instance);
					return instance;
				}

				@Override
				protected void remove(Statement stmt) {
					try {
						list = null;
						ContextAwareConnection conn = getConnection();
						SesameProperty.this.remove(conn, stmt);
					} catch (StoreException e) {
						throw new ElmoPersistException(e);
					}
				}

				@Override
				public void close() {
					if (list != null && (!hasNext()
							|| list.size() == CACHE_LIMIT)) {
						cache = list;
						cached = true;
					}
					super.close();
				}
			};
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
	}

}
