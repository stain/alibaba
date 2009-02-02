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
package org.openrdf.repository.object.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.object.traits.Mergeable;
import org.openrdf.repository.object.traits.Refreshable;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * This behaviour provides a java.util.List interface for RDF containers.
 * 
 * @author James Leigh
 */
public abstract class RDFSContainer extends AbstractList<Object> implements
		Refreshable, Mergeable, RDFObject {

	private static final int UNKNOWN = -1;

	private static final int BSIZE = 64;

	private volatile int _size = UNKNOWN;

	private List<Object[]> blocks = new ArrayList<Object[]>();

	public void refresh() {
		_size = UNKNOWN;
	}

	@Override
	public Object get(int index) {
		try {
			int b = index / BSIZE;
			Object[] block = getBlock(b);
			if (block != null)
				return block[index % BSIZE];
			Object[] list = loadBlock(b);
			assignBlock(b, list);
			return list[index % BSIZE];
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
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
	public Object set(int index, Object obj) {
		RepositoryConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			Object old = getAndSet(index, obj);
			if (autoCommit)
				conn.setAutoCommit(true);
			return old;
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
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
				for (int i = 0, n = size; i < n; i++) {
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
			conn.removeMatch(getResource(), pred, null);
			Object[] block = getBlock((size - 1) / BSIZE);
			if (block != null) {
				block[(size - 1) % BSIZE] = null;
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
	public void clear() {
		try {
			ContextAwareConnection conn = getObjectConnection();
			Resource resource = getResource();
			int size = _size;
			if (size < 0) {
				size = (int) conn.sizeMatch(resource, null, null);
			}
			for (int i = 0; i < size; i++) {
				URI pred = getMemberPredicate(i);
				conn.removeMatch(resource, pred, null);
			}
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	@Override
	public int size() {
		try {
			if (_size < 0) {
				synchronized (this) {
					if (_size < 0) {
						int index = findSize();
						_size = index;
					}
				}
			}
			return _size;
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	@intercepts(method = "toString", argc = 0)
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

	private int getIndex(URI pred) {
		assert pred.stringValue().startsWith(RDF.NAMESPACE + '_');
		return Integer.parseInt(pred.getLocalName().substring(1)) - 1;
	}

	private Object getAndSet(int index, Object o) throws StoreException {
		if (o == null)
			throw new NullPointerException();
		URI pred = getMemberPredicate(index);
		Object old = get(index);
		ObjectConnection conn = getObjectConnection();
		if (old != null) {
			conn.removeMatch(getResource(), pred, null);
		}
		conn.add(getResource(), pred, conn.addObject(o));
		Object[] block = getBlock(index / BSIZE);
		if (block != null) {
			block[index % BSIZE] = o;
		}
		return old;
	}

	private void assign(int index, Object o) throws StoreException {
		if (o == null)
			throw new NullPointerException();
		URI pred = getMemberPredicate(index);
		Value newValue = getObjectConnection().addObject(o);
		ContextAwareConnection conn = getObjectConnection();
		conn.add(getResource(), pred, newValue);
		clearBlock(index / BSIZE);
	}

	private void replace(int index, Object o) throws StoreException {
		if (o == null)
			throw new NullPointerException();
		URI pred = getMemberPredicate(index);
		ContextAwareConnection conn = getObjectConnection();
		Value newValue = getObjectConnection().addObject(o);
		boolean autoCommit = conn.isAutoCommit();
		if (autoCommit)
			conn.setAutoCommit(false);
		conn.removeMatch(getResource(), pred, null);
		conn.add(getResource(), pred, newValue);
		if (autoCommit)
			conn.setAutoCommit(true);
		Object[] block = getBlock(index / BSIZE);
		if (block != null) {
			block[index % BSIZE] = o;
		}
	}

	private int findSize() throws StoreException {
		ContextAwareConnection conn = getObjectConnection();
		long estimation = conn.sizeMatch(getResource(), null, null);
		int size = (int) estimation;
		while (size > 0 && get(size - 1) == null) {
			size--;
		}
		return size;
	}

	private synchronized Object[] getBlock(int b) {
		if (blocks.size() > b)
			return blocks.get(b);
		return null;
	}

	private synchronized void assignBlock(int b, Object[] list) {
		while (blocks.size() <= b) {
			blocks.add(null);
		}
		blocks.set(b, list);
	}

	private synchronized void clearBlock(int b) {
		if (blocks.size() > b) {
			blocks.set(b, null);
		}
	}

	private Object[] loadBlock(int b) throws StoreException {
		TupleQuery query = createBlockQuery(b);
		TupleResult result = query.evaluate();
		BindingSet bindings = result.next();
		ObjectFactory of = getObjectConnection().getObjectFactory();
		Object[] list = new Object[BSIZE];
		while (bindings != null) {
			URI pred = (URI) bindings.getValue("pred");
			int idx = getIndex(pred);
			Value value = bindings.getValue("value");
			List<URI> types = new ArrayList<URI>();
			do {
				Value c = bindings.getValue("value_class");
				if (c instanceof URI) {
					types.add((URI) c);
				}
				bindings = result.next();
			} while (bindings != null && pred.equals(bindings.getValue("pred")));
			int i = idx % BSIZE;
			if (value instanceof Literal) {
				list[i] = of.createObject((Literal) value);
			} else {
				list[i] = of.createRDFObject((Resource) value, types);
			}
		}
		return list;
	}

	private TupleQuery createBlockQuery(int b) throws StoreException {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ?pred ?value ?value_class\n");
		sb.append("WHERE { $self ?pred ?value\n");
		sb.append("OPTIONAL { ?value a ?value_class }\n");
		sb.append("FILTER (");
		for (int i = b * BSIZE, n = b * BSIZE + BSIZE; i < n; i++) {
			sb.append("?pred = <");
			sb.append(RDF.NAMESPACE);
			sb.append("_");
			sb.append((i + 1));
			sb.append(">");
			if (i + 1 < n) {
				sb.append(" || ");
			}
		}
		sb.append(")}\n");
		ObjectConnection con = getObjectConnection();
		TupleQuery query = con.prepareTupleQuery(SPARQL, sb.toString());
		query.setBinding("self", getResource());
		return query;
	}
}
