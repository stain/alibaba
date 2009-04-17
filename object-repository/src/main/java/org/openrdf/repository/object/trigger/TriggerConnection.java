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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.result.Result;

public class TriggerConnection extends RepositoryConnectionWrapper {

	private Map<URI, Set<Trigger>> triggers;

	private Map<URI, Map<Resource, Set<Value>>> events = new HashMap<URI, Map<Resource, Set<Value>>>();

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
	protected boolean isDelegatingAdd() throws RepositoryException {
		return false;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		boolean fire = false;
		if (triggers.containsKey(predicate)) {
			synchronized (events) {
				Map<Resource, Set<Value>> map = events.get(predicate);
				if (map == null) {
					events.put(predicate, map = new HashMap<Resource, Set<Value>>());
				}
				Set<Value> set = map.get(subject);
				if (set == null) {
					map.put(subject, set = new HashSet<Value>());
				}
				set.add(object);
				fire = isAutoCommit();
			}
		}
		if (fire) {
			setAutoCommit(false);
			try {
				getDelegate().add(subject, predicate, object, contexts);
				setAutoCommit(true);
			} finally {
				if (!isAutoCommit()) {
					rollback();
				}
			}
		} else {
			getDelegate().add(subject, predicate, object, contexts);
		}
	}

	@Override
	public void commit() throws RepositoryException {
		try {
			fireEvents();
		} catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
		super.commit();
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		if (autoCommit) {
			try {
				fireEvents();
			} catch (QueryEvaluationException e) {
				throw new RepositoryException(e);
			}
		}
		super.setAutoCommit(autoCommit);
	}

	private void fireEvents() throws RepositoryException, QueryEvaluationException {
		synchronized (events) {
			for (URI pred : events.keySet()) {
				Trigger sample = findBestTrigger(triggers.get(pred));
				Map<Resource, Set<Value>> statements = events.get(pred);
				String sparql = sample.getSparqlSubjectQuery();
				Result<Object> result = loadObjects(sparql, statements.keySet());
				try {
					while (result.hasNext()) {
						RDFObject subj = (RDFObject) result.next();
						for (Trigger trigger : triggers.get(pred)) {
							invokeTrigger(trigger, subj, pred, statements.get(subj.getResource()));
						}
					}
				} finally {
					result.close();
				}
			}
			events.clear();
		}
	}

	private Result<Object> loadObjects(String sparqlBase,
			Collection<? extends Value> values) throws RepositoryException,
			QueryEvaluationException {
		String sparql = buildQuery(sparqlBase, values.size());
		try {
			ObjectQuery query = objects.prepareObjectQuery(SPARQL, sparql);
			Iterator<? extends Value> iter = values.iterator();
			for (int i = 0; iter.hasNext(); i++) {
				query.setBinding("_" + i, iter.next());
			}
			return query.evaluate(Object.class);
		} catch (MalformedQueryException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private void invokeTrigger(Trigger trigger, Object subj, URI pred, Set<Value> objs)
			throws RepositoryException, QueryEvaluationException {
		try {
			String name = trigger.getMethodName();
			Class<?>[] types = trigger.getParameterTypes();
			int idx = trigger.getParameterIndex(pred);
			if (idx < 0 && types.length > 0) {
				logger.warn("No parameter for predicate: {} in {}", pred, trigger.getMethodName());
				return;
			}
			Method method = subj.getClass().getMethod(name, types);
			Object[] args = new Object[types.length];
			if (types.length == 0) {
				method.invoke(subj);
			} else if (types[idx].equals(Set.class) && containsResource(objs)) {
				args[idx] = loadObjects(trigger.getSparqlObjectQuery(idx), objs).asSet();
				method.invoke(subj, args);
			} else if (types[idx].equals(Set.class)) {
				Set<Object> arg = new HashSet<Object>(objs.size());
				for (Value obj : objs) {
					arg.add(objects.getObject(obj));
				}
				args[idx] = arg;
				method.invoke(subj, args);
			} else {
				args[idx] = objects.getObject(objs.iterator().next());
				method.invoke(subj, args);
			}
		} catch (SecurityException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalArgumentException e) {
			throw new ObjectCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectCompositionException(e);
		} catch (InvocationTargetException e) {
			throw new RepositoryException(e.getCause());
		} catch (NoSuchMethodException e) {
			// skip trigger
		}
	}

	private boolean containsResource(Set<Value> objs) {
		for (Value obj : objs) {
			if (obj instanceof Resource) {
				return true;
			}
		}
		return false;
	}

	private Trigger findBestTrigger(Set<Trigger> set) {
		Trigger best = null;
		int rank = 0;
		for (Trigger trigger : set) {
			int size = trigger.getSparqlSubjectQuery().length();
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
