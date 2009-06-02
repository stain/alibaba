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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.result.Result;

/**
 * Wrapper used when triggers have been registered with the connection.
 * 
 * @author James Leigh
 * 
 */
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
				Trigger sample = triggers.get(pred).iterator().next();
				Map<Resource, Set<Value>> statements = events.get(pred);
				Resource[] resources = statements.keySet().toArray(new Resource[statements.size()]);
				Result<?> result = objects.getObjects(sample.getDeclaredIn(), resources);
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
			boolean containsLiteral = containsLiteral(objs);
			if (types.length == 0) {
				// no arguments
			} else if (types[idx].equals(Set.class) && !containsLiteral) {
				// TODO get parameterized type
				Resource[] resources = objs.toArray(new Resource[objs.size()]);
				args[idx] = objects.getObjects(types[idx], resources).asSet();
			} else if (types[idx].equals(Set.class)) {
				Set<Object> arg = new HashSet<Object>(objs.size());
				for (Value obj : objs) {
					arg.add(objects.getObject(obj));
				}
				args[idx] = arg;
			} else if (!containsLiteral) {
				Resource resource = (Resource) objs.iterator().next();
				List<?> result = objects.getObjects(types[idx], resource).asList();
				args[idx] = result.get(0);
			} else {
				args[idx] = objects.getObject(objs.iterator().next());
			}
			method.invoke(subj, args);
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

	private boolean containsLiteral(Set<Value> objs) {
		for (Value obj : objs) {
			if (obj instanceof Literal) {
				return true;
			}
		}
		return false;
	}

}
