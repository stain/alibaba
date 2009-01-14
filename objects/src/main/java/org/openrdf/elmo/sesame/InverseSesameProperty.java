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
package org.openrdf.elmo.sesame;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.elmo.exceptions.ElmoPersistException;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareConnection;

/**
 * A set for a given getResource(), predicate.
 * 
 * @author James Leigh
 * 
 * @param <E>
 */
public class InverseSesameProperty<E> extends SesameProperty<E> {

	public InverseSesameProperty(SesameEntity bean, PropertyChanger property) {
		super(bean, property);
	}

	@Override
	public void clear() {
		ContextAwareConnection conn = getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			conn.setAutoCommit(false);
			CloseableIteration<? extends Statement, RepositoryException> stmts;
			stmts = getStatements();
			try {
				while (stmts.hasNext()) {
					remove(conn, stmts.next());
				}
			} finally {
				stmts.close();
			}
			conn.setAutoCommit(autoCommit);
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
		refreshCache();
		refreshEntity();
	}

	@Override
	public boolean add(Object o) {
		Value val = getValue(o);
		if (contains(val))
			return false;
		ContextAwareConnection conn = getConnection();
		try {
			add(conn, (Resource) val, getResource());
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
		return true;
	}

	@Override
	public boolean remove(Object o) {
		Value val = getValue(o);
		if (!contains(val))
			return false;
		ContextAwareConnection conn = getConnection();
		try {
			remove(conn, (Resource) val, getResource());
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
		return true;
	}

	@Override
	public boolean contains(Object o) {
		Value val = getValue(o);
		ContextAwareConnection conn = getConnection();
		try {
			return conn.hasStatement((Resource) val, getURI(), getResource());
		} catch (RepositoryException e) {
			throw new ElmoPersistException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	E createInstance(Statement stmt) {
		Value value = stmt.getSubject();
		return (E) getManager().getInstance(value);
	}

	@Override
	CloseableIteration<? extends Statement, RepositoryException> getStatements()
			throws RepositoryException {
		ContextAwareConnection conn = getConnection();
		return conn.getStatements(null, getURI(), getResource());
	}

}
