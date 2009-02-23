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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.ObjectPersistException;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.object.traits.ManagedRDFObject;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

/**
 * SesameProperty used for localized properties. Only the best set of literals
 * are included in the results.
 * 
 * @author James Leigh
 */
public class LocalizedPropertySet extends CachedPropertySet {

	public LocalizedPropertySet(ManagedRDFObject bean, PropertySetModifier property) {
		super(bean, property);
	}

	@Override
	public void clear() {
		ContextAwareConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.begin();
			for (Literal lit : bestValues()) {
				remove(conn, getResource(), lit);
			}
			if (autoCommit)
				conn.commit();
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
		}
	}

	@Override
	public String getSingle() {
		Iterator<Literal> iter = bestValues().iterator();
		try {
			if (iter.hasNext())
				return (String) createInstance(iter.next());
			return null;
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	public boolean isEmpty() {
		Iterator<Literal> iter = bestValues().iterator();
		return iter.hasNext();
	}

	@Override
	public Iterator<Object> iterator() {
		return new Iterator<Object>() {
			private Iterator<Literal> iter = bestValues().iterator();

			private Literal lit;

			public boolean hasNext() {
				return iter.hasNext();
			}

			public Object next() {
				try {
					return createInstance(lit = iter.next());
				} catch (StoreException e) {
					throw new ObjectStoreException(e);
				}
			}

			public void remove() {
				try {
					ContextAwareConnection conn = getObjectConnection();
					LocalizedPropertySet.this.remove(conn, getResource(), lit);
				} catch (StoreException e) {
					throw new ObjectPersistException(e);
				}
			}

		};
	}

	@Override
	public void setSingle(Object o) {
		if (o == null) {
			clear();
		} else {
			setAll(Collections.singleton(o));
		}
	}

	@Override
	public void setAll(Set<?> set) {
		if (this == set)
			return;
		Set<Object> c = new HashSet<Object>(set);
		ContextAwareConnection conn = getObjectConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.begin();
			String language = getObjectConnection().getLanguage();
			ModelResult stmts;
			stmts = getStatements();
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Literal lit = (Literal) stmt.getObject();
					String l = lit.getLanguage();
					if (language == l || language != null && language.equals(l)) {
						Object next = createInstance(lit);
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
				conn.commit();
		} catch (StoreException e) {
			throw new ObjectPersistException(e);
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
		ObjectConnection con = getObjectConnection();
		String lang = con.getLanguage();
		ValueFactory vf = con.getValueFactory();
		return vf.createLiteral(instance.toString(), lang);
	}

	Collection<Literal> bestValues() {
		int score = -1;
		Collection<Literal> values = new ArrayList<Literal>();
		String language = getObjectConnection().getLanguage();
		Cursor<Value> stmts;
		try {
			stmts = getValues();
			try {
				Value value;
				while ((value = stmts.next()) != null) {
					score = addBestStatements(value, language, score, values);
				}
			} finally {
				stmts.close();
			}
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
		return values;
	}

	private int addBestStatements(Value value, String language, int best,
			Collection<Literal> values) {
		int score = best;
		Literal lit = (Literal) value;
		String l = lit.getLanguage();
		if (language == l || language != null && language.equals(l)) {
			if (score < Integer.MAX_VALUE)
				values.clear();
			values.add(lit);
			score = Integer.MAX_VALUE;
		} else if (l != null && language != null && language.startsWith(l)) {
			if (score < l.length())
				values.clear();
			values.add(lit);
			score = l.length();
		} else if (l != null && language != null && score <= 1
				&& l.length() > 2 && language.startsWith(l.substring(0, 2))) {
			if (score < 1)
				values.clear();
			values.add(lit);
			score = 1;
		} else if (l == null) {
			if (score < 0)
				values.clear();
			values.add(lit);
			score = 0;
		} else if (score < 0) {
			values.add(lit);
		}
		return score;
	}

}
