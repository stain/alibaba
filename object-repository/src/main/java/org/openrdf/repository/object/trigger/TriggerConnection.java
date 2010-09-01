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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;

/**
 * Wrapper used when triggers have been registered with the connection.
 * 
 * @author James Leigh
 * 
 */
public class TriggerConnection extends RepositoryConnectionWrapper {

	private static final int MAX_TRIG_STACK = 100;
	private Map<URI, Set<Trigger>> triggers;
	private Map<URI, Map<Resource, Set<Value>>> events = new HashMap<URI, Map<Resource, Set<Value>>>();
	private ObjectConnection objects;
	private ObjectFactory of;

	public TriggerConnection(RepositoryConnection delegate,
			Map<URI, Set<Trigger>> triggers) {
		super(delegate.getRepository(), delegate);
		this.triggers = triggers;
	}

	public void setObjectConnection(ObjectConnection objects) {
		this.objects = objects;
		of = objects.getObjectFactory();
	}

	@Override
	protected boolean isDelegatingAdd() throws RepositoryException {
		return false;
	}

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate,
			Value object, Resource... contexts) throws RepositoryException {
		boolean containsKey = triggers.containsKey(predicate);
		if (containsKey && isAutoCommit()) {
			setAutoCommit(false);
			try {
				getDelegate().add(subject, predicate, object, contexts);
				recordEvent(subject, predicate, object);
				setAutoCommit(true);
			} finally {
				if (!isAutoCommit()) {
					rollback();
				}
			}
		} else {
			getDelegate().add(subject, predicate, object, contexts);
			if (containsKey) {
				recordEvent(subject, predicate, object);
			}
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

	private synchronized void recordEvent(Resource subject, URI predicate,
			Value object) {
		Map<Resource, Set<Value>> map = events.get(predicate);
		if (map == null) {
			map = new HashMap<Resource, Set<Value>>();
			events.put(predicate, map);
		}
		Set<Value> set = map.get(subject);
		if (set == null) {
			map.put(subject, set = new HashSet<Value>());
		}
		set.add(object);
	}

	private synchronized void fireEvents() throws RepositoryException,
			QueryEvaluationException {
		for (int i = 0; !events.isEmpty() && i < MAX_TRIG_STACK; i++) {
			Map<URI, Map<Resource, Set<Value>>> firedEvents = events;
			events = new HashMap(firedEvents.size());
			for (URI pred : firedEvents.keySet()) {
				Map<Resource, Set<Value>> map = firedEvents.get(pred);
				Set<Trigger> set = triggers.get(pred);
				Trigger sample = set.iterator().next();
				Class<?> declaredIn = sample.getDeclaredIn();
				Set<URI> roles = new HashSet<URI>(4);
				Set<URI> types = getTypes(declaredIn, roles);
				for (Map.Entry<Resource, Set<Value>> e : map.entrySet()) {
					Object subj = of.createObject(e.getKey(), types);
					for (Trigger trigger : set) {
						invokeTrigger(trigger, subj, pred, e.getValue());
					}
				}
			}
		}
		if (!events.isEmpty()) {
			events.clear();
			throw new RepositoryException("Trigger Overflow");
		}
	}

	private <C extends Collection<URI>> C getTypes(Class<?> role, C set)
			throws RepositoryException {
		URI type = of.getNameOf(role);
		if (type == null) {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null) {
				getTypes(superclass, set);
			}
			Class<?>[] interfaces = role.getInterfaces();
			for (int i = 0, n = interfaces.length; i < n; i++) {
				getTypes(interfaces[i], set);
			}
		} else {
			set.add(type);
		}
		return set;
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
				method.invoke(subj, args);
			} else if (types[idx].equals(Set.class) && !containsLiteral) {
				// TODO get parameterized type
				Resource[] resources = objs.toArray(new Resource[objs.size()]);
				args[idx] = objects.getObjects(types[idx], resources).asSet();
				method.invoke(subj, args);
			} else if (types[idx].equals(Set.class)) {
				Set<Object> arg = new HashSet<Object>(objs.size());
				for (Value obj : objs) {
					arg.add(objects.getObject(obj));
				}
				args[idx] = arg;
				method.invoke(subj, args);
			} else if (!containsLiteral) {
				// TODO get parameterized type
				Resource[] resources = objs.toArray(new Resource[objs.size()]);
				for (Object arg : objects.getObjects(types[idx], resources).asSet()) {
					args[idx] = arg;
					method.invoke(subj, args);
				}
			} else {
				for (Value obj : objs) {
					args[idx] = objects.getObject(obj);
					method.invoke(subj, args);
				}
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
			logger.debug("{} has no trigger {}", subj, trigger.getMethodName());
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
