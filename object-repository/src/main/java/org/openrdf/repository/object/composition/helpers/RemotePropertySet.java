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
package org.openrdf.repository.object.composition.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.cursor.ConvertingCursor;
import org.openrdf.cursor.Cursor;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.object.result.ObjectIterator;
import org.openrdf.repository.object.traits.Refreshable;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

/**
 * A set for a given getResource(), predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 */
public class RemotePropertySet implements PropertySet, Set<Object> {
	private final RDFObject bean;
	protected PropertySetModifier property;

	public RemotePropertySet(RDFObject bean, PropertySetModifier property) {
		assert bean != null;
		assert property != null;
		this.bean = bean;
		this.property = property;
	}

	public void refresh() {
		// no-op
	}

	/**
	 * This method always returns <code>true</code>
	 * 
	 * @return <code>true</code>
	 */
	public boolean add(Object o) {
		ContextAwareConnection conn = getObjectConnection();
		try {
			add(conn, getResource(), getValue(o));
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
		refreshEntity();
		refresh(o);
		return true;
	}

	public boolean addAll(Collection<?> c) {
		RepositoryConnection conn = getObjectConnection();
		boolean modified = false;
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			for (Object o : c)
				if (add(o))
					modified = true;
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
		refreshEntity();
		return modified;
	}

	public void clear() {
		try {
			property.remove(getObjectConnection(), getResource(), null);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
		refreshEntity();
	}

	public boolean contains(Object o) {
		ContextAwareConnection conn = getObjectConnection();
		try {
			Value val = getValue(o);
			return conn.hasMatch(getResource(), getURI(), val);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public boolean containsAll(Collection<?> c) {
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
		RemotePropertySet p = (RemotePropertySet) o;
		if (!getResource().equals(p.getResource()))
			return false;
		if (!property.equals(p.property))
			return false;
		if (!getObjectConnection().equals(p.getObjectConnection()))
			return false;
		return true;
	}

	public Set<Object> getAll() {
		return this;
	}

	public Object getSingle() {
		ObjectIterator<?, Object> iter = getObjectIterator();
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
		ObjectIterator<?, Object> iter = getObjectIterator();
		try {
			return !iter.hasNext();
		} finally {
			iter.close();
		}
	}

	/**
	 * This method always returns <code>true</code>
	 * 
	 * @return <code>true</code>
	 */
	public boolean remove(Object o) {
		ContextAwareConnection conn = getObjectConnection();
		try {
			remove(conn, getResource(), getValue(o));
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
		refresh(o);
		refreshEntity();
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		RepositoryConnection conn = getObjectConnection();
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
			throw new ObjectPersistException(e);
		}
		refreshEntity();
		return modified;
	}

	public boolean retainAll(Collection<?> c) {
		RepositoryConnection conn = getObjectConnection();
		boolean modified = false;
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			ObjectIterator<?, Object> e = getObjectIterator();
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
			throw new ObjectPersistException(e);
		}
		refreshEntity();
		return modified;
	}

	public void setAll(Set<?> set) {
		if (this == set)
			return;
		if (set == null) {
			clear();
			return;
		}
		Set<Object> c = new HashSet<Object>(set);
		RepositoryConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			clear();
			addAll(c);
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	public void setSingle(Object o) {
		if (o == null) {
			clear();
		} else {
			RepositoryConnection conn = getObjectConnection();
			try {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit)
					conn.setAutoCommit(false);
				clear();
				add(o);
				if (autoCommit)
					conn.setAutoCommit(true);
			} catch (StoreException e) {
				throw new ObjectPersistException(e);
			}
		}
	}

	public int size() {
		try {
			ContextAwareConnection conn = getObjectConnection();
			return (int) conn.sizeMatch(getResource(), getURI(), null);
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

	public Iterator<Object> iterator() {
		return getObjectIterator();
	}

	public Object[] toArray() {
		List<Object> list = new ArrayList<Object>();
		ObjectIterator<?, Object> iter = getObjectIterator();
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
		List<Object> list = new ArrayList<Object>();
		ObjectIterator<?, Object> iter = getObjectIterator();
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
		ObjectIterator<?, Object> iter = getObjectIterator();
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

	final ObjectConnection getObjectConnection() {
		return bean.getObjectConnection();
	}

	final Resource getResource() {
		return bean.getResource();
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

	protected Object createInstance(Value value) throws StoreException {
		if (value instanceof Resource)
			return getObjectConnection().getObject((Resource) value);
		return getObjectConnection().getObjectFactory().createObject(
				((Literal) value));
	}

	protected ModelResult getStatements() throws StoreException {
		ContextAwareConnection conn = getObjectConnection();
		return conn.match(getResource(), getURI(), null);
	}

	protected Cursor<Value> getValues() throws StoreException {
		return new ConvertingCursor<Statement, Value>(getStatements()) {
			@Override
			protected Value convert(Statement st)
					throws StoreException {
				return st.getObject();
			}
		};
	}

	protected Value getValue(Object instance) throws StoreException {
		return getObjectConnection().addObject(instance);
	}

	protected void refresh(Object o) {
		if (o instanceof Refreshable) {
			((Refreshable) o).refresh();
		}
	}

	protected void refreshEntity() {
		refresh();
		refresh(bean);
	}

	protected Cursor<?> getObjects() throws StoreException {
		return new ConvertingCursor<Value, Object>(getValues()) {

			@Override
			protected Object convert(Value value) throws StoreException {
				return createInstance(value);
			}
		};
	}

	protected ObjectIterator<?, Object> getObjectIterator() {
		try {
			return new ObjectIterator<Object, Object>(getObjects()) {

				@Override
				protected void remove(Object o) {
					RemotePropertySet.this.remove(o);
				}
			};
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

}
