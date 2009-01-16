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
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.results.ObjectIterator;
import org.openrdf.store.StoreException;

/**
 * A set for a given getResource(), predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 */
public class CachedPropertySet<E> extends RemotePropertySet<E> {
	private static final int CACHE_LIMIT = 10;
	List<E> cache;
	boolean cached;

	public CachedPropertySet(RDFObject bean, PropertySetModifier property) {
		super(bean, property);
	}

	@Override
	public void refresh() {
		super.refresh();
		cached = false;
		cache = null;
	}

	@Override
	public void clear() {
		if (!cached || !cache.isEmpty()) {
			super.clear();
			refreshCache();
		}
	}

	@Override
	public boolean contains(Object o) {
		if (isCacheComplete())
			return cache.contains(o);
		if (cached && cache.contains(o))
			return true;
		return super.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (isCacheComplete())
			return cache.containsAll(c);
		if (cached && cache.containsAll(c))
			return true;
		return super.containsAll(c);
	}

	@Override
	public E getSingle() {
		if (cached && cache.isEmpty())
			return null;
		if (cached)
			return cache.get(0);
		return super.getSingle();
	}

	@Override
	public boolean isEmpty() {
		if (cached)
			return cache.isEmpty();
		return super.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = super.removeAll(c);
		refreshCache();
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = super.retainAll(c);
		refreshCache();
		return modified;
	}

	@Override
	public int size() {
		if (isCacheComplete())
			return cache.size();
		return super.size();
	}

	@Override
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
					CachedPropertySet.this.remove(e);
				}
			};
		}
		return super.iterator();
	}

	@Override
	public Object[] toArray() {
		if (isCacheComplete())
			return cache.toArray();
		return super.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (isCacheComplete())
			return cache.toArray(a);
		return super.toArray(a);
	}

	protected void refreshCache() {
		if (cached) {
			for (E e : cache) {
				refresh(e);
			}
		}
	}

	private boolean isCacheComplete() {
		return cached && cache.size() < CACHE_LIMIT;
	}

	@Override
	protected ObjectIterator<Statement, E> getObjectIterator() {
		try {
			return new ObjectIterator<Statement, E>(getStatements()) {
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
						ContextAwareConnection conn = getObjectConnection();
						CachedPropertySet.this.remove(conn, stmt);
					} catch (StoreException e) {
						throw new ObjectPersistException(e);
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
			throw new ObjectPersistException(e);
		}
	}

}
