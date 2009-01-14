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
package org.openrdf.elmo.sesame.behaviours;

import info.aduna.iteration.CloseableIteration;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.openrdf.elmo.Entity;
import org.openrdf.elmo.Mergeable;
import org.openrdf.elmo.Refreshable;
import org.openrdf.elmo.annotations.intercepts;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.exceptions.ElmoIOException;
import org.openrdf.elmo.exceptions.ElmoPersistException;
import org.openrdf.elmo.sesame.SesameManager;
import org.openrdf.elmo.sesame.iterators.ElmoIteration;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareConnection;

/**
 * Java instance for rdf:List as a familiar interface to manipulate this List.
 * This implemention can only be modified when in autoCommit (autoFlush), or
 * when read uncommitted is supported.
 * 
 * @author James Leigh
 */
@rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
public class SesameList extends AbstractSequentialList<Object> implements
		java.util.List<Object>, Refreshable, Mergeable {

	SesameManager manager;

	Resource resource;

	private int _size = -1;

	private SesameList parent;

	public SesameList(Entity bean) {
		SesameEntity sbean = (SesameEntity) bean;
		this.manager = sbean.getSesameManager();
		this.resource = sbean.getSesameResource();
	}

	public void refresh() {
		_size = -1;
		if (parent != null)
			parent.refresh();
	}

	public void merge(Object source) {
		if (source instanceof java.util.List) {
			clear();
			addAll((java.util.List) source);
		}
	}

	ValueFactory getValueFactory() {
		RepositoryConnection conn = manager.getConnection();
		return conn.getRepository().getValueFactory();
	}

	private ElmoIteration<Statement, Value> getStatements(Resource subj,
			URI pred, Value obj) {
		try {
			CloseableIteration<? extends Statement, RepositoryException> stmts;
			ContextAwareConnection conn = manager.getConnection();
			stmts = conn.getStatements(subj, pred, obj);
			return new ElmoIteration<Statement, Value>(stmts) {
				@Override
				protected Value convert(Statement stmt) throws Exception {
					return stmt.getObject();
				}
			};
		} catch (RepositoryException e) {
			throw new ElmoIOException(e);
		}
	}

	void addStatement(Resource subj, URI pred, Value obj) {
		if (obj == null)
			return;
		try {
			ContextAwareConnection conn = manager.getConnection();
			conn.add(subj, pred, obj);
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	void removeStatements(Resource subj, URI pred, Value obj) {
		try {
			ContextAwareConnection conn = manager.getConnection();
			conn.remove(subj, pred, obj);
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	@Override
	public int size() {
		if (_size < 0) {
			synchronized (this) {
				if (_size < 0) {
					Resource list = resource;
					int size;
					for (size = 0; list != null && !list.equals(RDF.NIL); size++) {
						Resource nlist = getRest(list);
						if (nlist == null && getFirst(list) == null)
							break;
						list = nlist;
					}
					_size = size;
				}
			}
		}
		return _size;
	}

	@Override
	public ListIterator<Object> listIterator(final int index) {
		return new ListIterator<Object>() {
			private ArrayList<Resource> prevLists = new ArrayList<Resource>();

			private boolean removed;

			Resource list;
			{
				for (int i = 0; i < index; i++) {
					next();
				}
			}

			public void add(Object o) {
				RepositoryConnection conn = manager.getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit)
						conn.setAutoCommit(false);
					if (resource.equals(RDF.NIL)) {
						// size == 0
						throw new ElmoPersistException(
								"cannot add a value to the nil list");
						/*
						 * list = _id = getValueFactory().createBNode();
						 * addStatement(list, RDF.FIRST,
						 * SesameProperty.createValue(List.this, o));
						 * addStatement(list, RDF.REST, RDF.NIL);
						 */
					}
					Value value = o == null ? null : manager.getValue(o);
					if (getFirst(resource) == null) {
						// size == 0
						list = resource;
						addStatement(list, RDF.FIRST, value);
						addStatement(list, RDF.REST, RDF.NIL);
					} else if (list == null) {
						// index = 0
						Value first = getFirst(resource);
						Resource rest = getRest(resource);
						BNode newList = getValueFactory().createBNode();
						addStatement(newList, RDF.FIRST, first);
						addStatement(newList, RDF.REST, rest);
						removeStatements(resource, RDF.FIRST, first);
						removeStatements(resource, RDF.REST, rest);
						addStatement(resource, RDF.FIRST, value);
						addStatement(resource, RDF.REST, newList);
					} else if (!list.equals(RDF.NIL)) {
						Resource rest = getRest(list);
						BNode newList = getValueFactory().createBNode();
						removeStatements(list, RDF.REST, rest);
						addStatement(list, RDF.REST, newList);
						addStatement(newList, RDF.FIRST, value);
						addStatement(newList, RDF.REST, rest);
					} else {
						// index == size
						throw new NoSuchElementException();
					}
					if (autoCommit)
						conn.setAutoCommit(true);
					refresh();
				} catch (RepositoryException e) {
					throw new ElmoPersistException(e);
				}
			}

			public void set(Object o) {
				RepositoryConnection conn = manager.getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit)
						conn.setAutoCommit(false);
					if (resource.equals(RDF.NIL)) {
						// size == 0
						throw new NoSuchElementException();
					} else if (list.equals(RDF.NIL)) {
						// index = size
						throw new NoSuchElementException();
					} else {
						Value first = getFirst(list);
						removeStatements(list, RDF.FIRST, first);
						if (o != null) {
							Value obj = manager.getValue(o);
							addStatement(list, RDF.FIRST, obj);
						}
					}
					if (autoCommit)
						conn.setAutoCommit(true);
					refresh();
				} catch (RepositoryException e) {
					throw new ElmoPersistException(e);
				}
			}

			public void remove() {
				RepositoryConnection conn = manager.getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit)
						conn.setAutoCommit(false);
					if (prevLists.size() < 1) {
						// remove index == 0
						Value first = getFirst(list);
						removeStatements(list, RDF.FIRST, first);
						Resource next = getRest(list);
						first = getFirst(next);
						Resource rest = getRest(next);
						removeStatements(list, RDF.REST, next);
						if (first != null) {
							removeStatements(next, RDF.FIRST, first);
							addStatement(list, RDF.FIRST, first);
						}
						if (rest != null) {
							removeStatements(next, RDF.REST, rest);
							addStatement(list, RDF.REST, rest);
						}
					} else {
						// remove index > 0
						Resource removedList = list;
						list = prevLists.remove(prevLists.size() - 1);
						Value first = getFirst(removedList);
						Resource rest = getRest(removedList);
						removeStatements(removedList, RDF.FIRST, first);
						removeStatements(removedList, RDF.REST, rest);
						removeStatements(list, RDF.REST, removedList);
						addStatement(list, RDF.REST, rest);
					}
					if (autoCommit)
						conn.setAutoCommit(true);
					removed = true;
					refresh();
				} catch (RepositoryException e) {
					throw new ElmoPersistException(e);
				}
			}

			public boolean hasNext() {
				Resource next;
				if (list == null) {
					next = resource;
				} else {
					next = getRest(list);
				}
				return getFirst(next) != null;
			}

			public Object next() {
				if (list == null) {
					list = resource;
				} else if (!removed) {
					prevLists.add(list);
					list = getRest(list);
				} else {
					removed = false;
				}
				Value first = getFirst(list);
				if (first == null)
					throw new NoSuchElementException();
				return createInstance(first);
			}

			public int nextIndex() {
				if (list == null)
					return 0;
				return prevLists.size() + 1;
			}

			public int previousIndex() {
				return prevLists.size() - 1;
			}

			public boolean hasPrevious() {
				return prevLists.size() > 0;
			}

			public Object previous() {
				list = prevLists.remove(prevLists.size() - 1);
				removed = false;
				Value first = getFirst(list);
				if (first == null)
					throw new NoSuchElementException();
				return createInstance(first);
			}

			private Object createInstance(Value first) {
				return manager.getInstance(first);
			}
		};
	}

	@Override
	@intercepts(method="toString",argc=0)
	public String toString() {
		return super.toString();
	}

	Value getFirst(Resource list) {
		if (list == null)
			return null;
		ElmoIteration<Statement, Value> stmts;
		stmts = getStatements(list, RDF.FIRST, null);
		try {
			if (stmts.hasNext())
				return stmts.next();
			return null;
		} finally {
			stmts.close();
		}
	}

	Resource getRest(Resource list) {
		if (list == null)
			return null;
		ElmoIteration<Statement, Value> stmts;
		stmts = getStatements(list, RDF.REST, null);
		try {
			if (stmts.hasNext())
				return (Resource) stmts.next();
			return null;
		} finally {
			stmts.close();
		}
	}
}
