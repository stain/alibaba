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
package org.openrdf.repository.object.roles;

import java.util.AbstractList;
import java.util.HashSet;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.object.results.ObjectIterator;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

/**
 * This behaviour provides a java.util.List interface for RDF containers.
 * 
 * @author James Leigh
 */
public abstract class RDFSContainer extends AbstractList<Object> implements
		Refreshable, Mergeable, RDFObject {

	private static final int UNKNOWN = -1;

	private int _size = UNKNOWN;

	public void refresh() {
		_size = UNKNOWN;
	}

	public void merge(Object source) {
		if (source instanceof java.util.List) {
			RepositoryConnection conn = getObjectConnection();
			try {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit)
					conn.setAutoCommit(false);
				java.util.List list = (java.util.List) source;
				int size = list.size();
				for (int i=0,n=size; i<n; i++) {
					assign(i, list.get(i));
				}
				if (_size > UNKNOWN && _size < size)
					_size = size;
				if (autoCommit)
					conn.setAutoCommit(true);
			} catch (StoreException e) {
				throw new ObjectPersistException(e);
			}
		}
	}

	@Override
	public Object set(int index, Object obj) {
		RepositoryConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			Value value = getAndSet(index, obj);
			Object old = createInstance(value);
			if (autoCommit)
				conn.setAutoCommit(true);
			return old;
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	@Override
	public void add(int index, Object obj) {
		RepositoryConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			for (int i = size() - 1; i >= index; i--) {
				replace(i + 1, get(i));
			}
			replace(index, obj);
			if (_size > UNKNOWN)
				_size++;
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	@Override
	public Object remove(int index) {
		RepositoryConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			Object obj = get(index);
			int size = size();
			for (int i = index; i < size - 1; i++) {
				replace(i, get(i + 1));
			}
			URI pred = getMemberPredicate(size - 1);
			ObjectIterator<Statement, Value> stmts = getStatements(pred);
			try {
				while (stmts.hasNext()) {
					stmts.next();
					stmts.remove();
				}
			} finally {
				stmts.close();
			}
			if (_size > UNKNOWN)
				_size--;
			conn.setAutoCommit(autoCommit);
			return obj;
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	@Override
	public Object get(int index) {
		URI pred = getMemberPredicate(index);
		ObjectIterator<Statement, Value> stmts = getStatements(pred);
		try {
			if (stmts.hasNext()) {
				Value next = stmts.next();
				return createInstance(next);
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	@Override
	public int size() {
		if (_size < 0) {
			synchronized (this) {
				if (_size < 0) {
					int index = getSize();
					_size = index;
				}
			}
		}
		return _size;
	}

	@Override
	@intercepts(method="toString",argc=0)
	public String toString() {
		return super.toString();
	}

	private URI getMemberPredicate(int index) {
		RepositoryConnection conn = getObjectConnection();
		Repository repository;
		repository = conn.getRepository();
		String uri = RDF.NAMESPACE + '_' + (index + 1);
		return repository.getURIFactory().createURI(uri);
	}

	private Value getAndSet(int index, Object o) {
		URI pred = getMemberPredicate(index);
		ObjectIterator<Statement, Value> stmts = getStatements(pred);
		try {
			Value newValue = o == null ? null : getObjectConnection().valueOf(o);
			Value oldValue = null;
			while (stmts.hasNext()) {
				oldValue = stmts.next();
				if (newValue == null || !newValue.equals(oldValue))
					stmts.remove();
			}
			if (newValue != null && !newValue.equals(oldValue)) {
				ContextAwareConnection conn = getObjectConnection();
				conn.add(getResource(), pred, newValue);
			}
			return oldValue;
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		} finally {
			stmts.close();
		}
	}

	private void assign(int index, Object o) {
		URI pred = getMemberPredicate(index);
		try {
			Value newValue = o == null ? null : getObjectConnection().valueOf(o);
			ContextAwareConnection conn = getObjectConnection();
			conn.add(getResource(), pred, newValue);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	private void replace(int index, Object o) {
		URI pred = getMemberPredicate(index);
		Value newValue = o == null ? null : getObjectConnection().valueOf(o);
		ContextAwareConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			conn.removeMatch(getResource(), pred, null);
			conn.add(getResource(), pred, newValue);
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	private ObjectIterator<Statement, Value> getStatements(URI pred) {
		try {
			ModelResult stmts;
			final ContextAwareConnection conn = getObjectConnection();
			stmts = conn.match(getResource(), pred, null);
			return new ObjectIterator<Statement, Value>(stmts) {
				@Override
				protected Value convert(Statement stmt) throws StoreException {
					return stmt.getObject();
				}

				@Override
				protected void remove(Statement stmt) throws StoreException {
					conn.remove(stmt);
				}
			};
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

	private Object createInstance(Value next) {
		return getObjectConnection().find(next);
	}

	private int getSize() {
		try {
			ModelResult iter;
			HashSet<URI> set = new HashSet<URI>();
			ContextAwareConnection conn = getObjectConnection();
			iter = conn.match(getResource(), null, null);
			try {
				while (iter.hasNext()) {
					set.add(iter.next().getPredicate());
				}
			} finally {
				iter.close();
			}
			int index = 0;
			while (set.contains(getMemberPredicate(index)))
				index++;
			return index;
		} catch (RuntimeException e) {
			throw e;
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}
}
