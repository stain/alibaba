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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.elmo.exceptions.ElmoIOException;
import org.openrdf.elmo.exceptions.ElmoPersistException;
import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

/**
 * SesameProperty used for localized properties. Only the best set of literals
 * are included in the results.
 * 
 * @author James Leigh
 */
public class LocalizedSesameProperty extends SesameProperty<String> {

	public LocalizedSesameProperty(SesameEntity bean, PropertyChanger property) {
		super(bean, property);
	}

	@Override
	public void clear() {
		ContextAwareConnection conn = getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			for (Statement stmt : bestValues()) {
				remove(conn, stmt);
			}
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
	}

	@Override
	public String getSingle() {
		Iterator<Statement> iter = bestValues().iterator();
		if (iter.hasNext())
			return createInstance(iter.next());
		return null;
	}

	@Override
	public boolean isEmpty() {
		Iterator<Statement> iter = bestValues().iterator();
		return iter.hasNext();
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			private Iterator<Statement> iter = bestValues().iterator();

			private Statement stmt;

			public boolean hasNext() {
				return iter.hasNext();
			}

			public String next() {
				return createInstance(stmt = iter.next());
			}

			public void remove() {
				try {
					ContextAwareConnection conn = getConnection();
					LocalizedSesameProperty.this.remove(conn, stmt);
				} catch (StoreException e) {
					throw new ElmoPersistException(e);
				}
			}

		};
	}

	@Override
	public void setSingle(String o) {
		if (o == null) {
			clear();
		} else {
			setAll(Collections.singleton(o));
		}
	}

	@Override
	public void setAll(Set<String> set) {
		if (this == set)
			return;
		Set<String> c = new HashSet<String>(set);
		ContextAwareConnection conn = getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.setAutoCommit(false);
			String language = getManager().getLanguage();
			ModelResult stmts;
			stmts = getStatements();
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Literal lit = (Literal) stmt.getObject();
					String l = lit.getLanguage();
					if (language == l || language != null && language.equals(l)) {
						Object next = createInstance(stmt);
						if (c.contains(next)) {
							c.remove(next);
						} else {
							remove(conn, stmt);
						}
					}
				}
			} finally {
				stmts.close();
			}
			if (c.size() > 0)
				addAll(c);
			if (autoCommit)
				conn.setAutoCommit(true);
		} catch (StoreException e) {
			throw new ElmoPersistException(e);
		}
	}

	@Override
	public int size() {
		return bestValues().size();
	}

	@Override
	public Object[] toArray() {
		return bestValues().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return bestValues().toArray(a);
	}

	@Override
	protected Value getValue(Object instance) {
		return getManager().getLocalizedValue(instance);
	}

	Collection<Statement> bestValues() {
		int score = -1;
		Collection<Statement> values = new ArrayList<Statement>();
		String language = getManager().getLanguage();
		ModelResult stmts;
		try {
			stmts = getStatements();
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					score = addBestStatements(stmt, language, score, values);
				}
			} finally {
				stmts.close();
			}
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
		return values;
	}

	private int addBestStatements(Statement stmt, String language, int best,
			Collection<Statement> values) {
		int score = best;
		Literal lit = (Literal) stmt.getObject();
		String l = lit.getLanguage();
		if (language == l || language != null && language.equals(l)) {
			if (score < Integer.MAX_VALUE)
				values.clear();
			values.add(stmt);
			score = Integer.MAX_VALUE;
		} else if (l != null && language != null && language.startsWith(l)) {
			if (score < l.length())
				values.clear();
			values.add(stmt);
			score = l.length();
		} else if (l != null && language != null && score <= 1
				&& l.length() > 2 && language.startsWith(l.substring(0, 2))) {
			if (score < 1)
				values.clear();
			values.add(stmt);
			score = 1;
		} else if (l == null) {
			if (score < 0)
				values.clear();
			values.add(stmt);
			score = 0;
		} else if (score < 0) {
			values.add(stmt);
		}
		return score;
	}

}
