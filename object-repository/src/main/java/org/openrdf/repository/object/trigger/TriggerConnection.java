/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.trigger;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

public class TriggerConnection extends RepositoryConnectionWrapper {

	private Map<URI, Set<Trigger>> triggers;

	private Model events = new LinkedHashModel();

	private ObjectConnection objects;

	public TriggerConnection(RepositoryConnection delegate,
			Map<URI, Set<Trigger>> triggers) {
		super(delegate.getRepository(), delegate);
		this.triggers = triggers;
	}

	public void setObjectConnection(ObjectConnection objects) {
		this.objects = objects;
	}

	@Override
	protected boolean isDelegatingAdd() throws StoreException {
		return false;
	}

	@Override
	public void add(Resource subject, URI predicate, Value object,
			Resource... contexts) throws StoreException {
		boolean fire = false;
		if (triggers.containsKey(predicate)) {
			synchronized (events) {
				events.add(subject, predicate, RDF.NIL);
				fire = isAutoCommit();
			}
		}
		super.add(subject, predicate, object, contexts);
		if (fire) {
			fireEvents();
		}
	}

	@Override
	public void commit() throws StoreException {
		fireEvents();
		super.commit();
	}

	private void fireEvents() throws StoreException {
		synchronized (events) {
			for (URI pred : events.predicates()) {
				Trigger sample = findBestTrigger(triggers.get(pred));
				Set<Resource> subjects = events.filter(null, pred, null)
						.subjects();
				Result<Object> result = findTriggeredObjects(sample, subjects);
				try {
					while (result.hasNext()) {
						Object obj = result.next();
						for (Trigger trigger : triggers.get(pred)) {
							invokeTrigger(trigger, obj);
						}
					}
				} finally {
					result.close();
				}
			}
			events.clear();
		}
	}

	private Result<Object> findTriggeredObjects(Trigger trigger,
			Collection<Resource> subjects) throws StoreException {
		String sparql = buildQuery(trigger.getSparqlQuery(), subjects.size());
		ObjectQuery query = objects.prepareObjectQuery(SPARQL, sparql);
		Iterator<Resource> iter = subjects.iterator();
		for (int i = 0; iter.hasNext(); i++) {
			query.setBinding("_" + i, iter.next());
		}
		return query.evaluate(Object.class);
	}

	private void invokeTrigger(Trigger trigger, Object obj)
			throws StoreException {
		try {
			String name = trigger.getMethodName();
			Method method = obj.getClass().getMethod(name);
			method.invoke(obj);
		} catch (SecurityException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalArgumentException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectCompositionException(e);
		} catch (InvocationTargetException e) {
			throw new StoreException(e.getCause());
		} catch (NoSuchMethodException e) {
			// skip trigger
		}
	}

	private Trigger findBestTrigger(Set<Trigger> set) {
		Trigger best = null;
		int rank = 0;
		for (Trigger trigger : set) {
			int size = trigger.getSparqlQuery().length();
			if (best == null || size > rank) {
				best = trigger;
				rank = size;
			}
		}
		return best;
	}

	private String buildQuery(String sparql, int subjects) {
		StringBuilder sb = new StringBuilder(sparql.length() + 512);
		int idx = sparql.lastIndexOf('}');
		sb.append(sparql, 0, idx);
		sb.append(" FILTER (");
		for (int i = 0; i < subjects; i++) {
			if (i > 0) {
				sb.append(" || ");
			}
			sb.append("?_ = $_").append(i);
		}
		sb.append(")\n");
		sb.append(sparql, idx, sparql.length());
		return sb.toString();
	}

}
