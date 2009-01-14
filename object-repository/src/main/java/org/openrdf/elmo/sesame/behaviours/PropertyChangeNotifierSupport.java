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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.interceptor.InvocationContext;

import org.openrdf.elmo.sesame.roles.PropertyChangeNotifier;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.model.Resource;
import org.openrdf.repository.DelegatingRepositoryConnection;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.event.NotifyingRepositoryConnection;
import org.openrdf.repository.event.base.NotifyingRepositoryConnectionWrapper;
import org.openrdf.repository.event.base.RepositoryConnectionListenerAdapter;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.intercepts;
import org.openrdf.repository.object.exceptions.ElmoIOException;

/**
 * Keeps a (non-persistent) list of observers, shared by all beans with the same
 * subject. A strong reference is used for the subscribers and this does prevent
 * garbage collection.
 * 
 * @author James Leigh
 * 
 */
@SuppressWarnings("unchecked")
public class PropertyChangeNotifierSupport implements PropertyChangeNotifier {
	static Set<String> triggers = new HashSet<String>(Arrays.asList("add",
			"addAll", "clear", "remove", "removeAll", "retainAll"));

	static Constructor<?> proxySet;

	static Constructor<?> proxyIterator;
	static {
		try {
			ClassLoader cl = PropertyChangeNotifierSupport.class
					.getClassLoader();
			if (cl == null)
				cl = Thread.currentThread().getContextClassLoader();
			Class[] invoClass = new Class[] { InvocationHandler.class };
			Class[] setClass = new Class[] { Set.class };
			Class[] iterClass = new Class[] { Iterator.class };
			Class<?> proxySetClass = Proxy.getProxyClass(cl, setClass);
			Class<?> proxyIterClass = Proxy.getProxyClass(cl, iterClass);
			proxySet = proxySetClass.getConstructor(invoClass);
			proxyIterator = proxyIterClass.getConstructor(invoClass);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}

	private final class ConnectionListener extends
			RepositoryConnectionListenerAdapter {
		private NotifyingRepositoryConnection broadcaster;

		public ConnectionListener(RepositoryConnection conn) {
			broadcaster = findBroadcaster(conn);
			broadcaster.addRepositoryConnectionListener(this);
		}

		@Override
		public void close(RepositoryConnection conn) {
			firePropertyChange(null, null);
			broadcaster.removeRepositoryConnectionListener(this);
		}

		@Override
		public void setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
			firePropertyChange(null, null);
			broadcaster.removeRepositoryConnectionListener(this);
		}

		@Override
		public void commit(RepositoryConnection conn) {
			firePropertyChange(null, null);
			broadcaster.removeRepositoryConnectionListener(this);
		}

		@Override
		public void rollback(RepositoryConnection conn) {
			firePropertyChange(null, null);
			broadcaster.removeRepositoryConnectionListener(this);
		}
	}

	private class ModificationHandler implements InvocationHandler {
		private Object delegate;

		public ModificationHandler(Object delegate) {
			this.delegate = delegate;
		}

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			++stack;
			try {
				Object result = method.invoke(delegate, args);
				if (triggers.contains(method.getName()))
					fireEvent(null, null);
				if (method.getName().equals("iterator")) {
					ModificationHandler handler = new ModificationHandler(result);
					return proxyIterator.newInstance(new Object[] { handler });
				}
				return result;
			} finally {
				--stack;
			}
		}
	}

	private static Map<Repository, ConcurrentMap<Resource, PropertyChangeSupport>> managers = new WeakHashMap();

	public static void notifyAllListenersOf(ObjectConnection manager) {
		Repository repo = manager.getConnection().getRepository();
		Map<Resource, PropertyChangeSupport> map = managers.get(repo);
		if (map != null) {
			for (Resource resource : map.keySet()) {
				manager.refresh(manager.find(resource));
			}
		}
	}

	private ConcurrentMap<Resource, PropertyChangeSupport> beans;

	private boolean immediate;

	private Resource resource;

	private SesameEntity bean;

	private ConnectionListener listener;

	int stack;

	public PropertyChangeNotifierSupport(RDFObject elmo) {
		this.bean = (SesameEntity) elmo;
		this.resource = bean.getSesameResource();
		ObjectConnection manager = bean.getSesameManager();
		RepositoryConnection conn = manager.getConnection();
		immediate = findBroadcaster(conn) == null;
		Repository repository = conn.getRepository();
		synchronized (managers) {
			if (managers.containsKey(repository)) {
				beans = managers.get(repository);
			} else {
				managers.put(repository, beans = new ConcurrentHashMap());
			}
		}
	}

	@intercepts(method = "get.*", parameters = {}, returns = Set.class)
	public Object getCalled(InvocationContext ctx) throws Exception {
		Object result = ctx.proceed();
		ModificationHandler handler = new ModificationHandler(result);
		return proxySet.newInstance(new Object[] { handler });
	}

	@intercepts(method = "set.*", argc = 1, returns = Void.class)
	public Object setCalled(InvocationContext ctx) throws Exception {
		++stack;
		try {
			Object r = ctx.proceed();
			fireEvent(ctx.getMethod().getName(), ctx.getParameters()[0]);
			return r;
		} finally {
			--stack;
		}
	}

	@intercepts(method = "merge", argc = 1, returns = Void.class)
	public Object mergeCalled(InvocationContext ctx) throws Exception {
		++stack;
		try {
			Object r = ctx.proceed();
			fireEvent(null, null);
			return r;
		} finally {
			--stack;
		}
	}

	@intercepts(method = "refresh", argc = 0, returns = Void.class)
	public Object refreshCalled(InvocationContext ctx) throws Exception {
		Object r = ctx.proceed();
		if (stack == 0) {
			fireEvent(null, null);
		}
		return r;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		PropertyChangeSupport subscribers;
		if (beans.containsKey(resource)) {
			subscribers = beans.get(resource);
		} else {
			subscribers = new PropertyChangeSupport(resource);
			PropertyChangeSupport o = beans.putIfAbsent(resource, subscribers);
			if (o != null) {
				subscribers = o;
			}
		}
		subscribers.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (beans.containsKey(resource)) {
			PropertyChangeSupport subscribers = beans.get(resource);
			subscribers.removePropertyChangeListener(listener);
		}
	}

	void fireEvent(String setter, Object newValue) {
		if (immediate) {
			firePropertyChange(setter, newValue);
			return;
		}
		ObjectConnection manager = bean.getSesameManager();
		ContextAwareConnection conn = manager.getConnection();
		try {
			if (conn.isAutoCommit()) {
				firePropertyChange(setter, newValue);
				return;
			}
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}

		if (listener == null)
			listener = new ConnectionListener(conn);
	}

	void firePropertyChange(String setter, Object newValue) {
		listener = null;
		if (beans.containsKey(resource)) {
			String property = null;
			if (setter != null) {
				char c = Character.toLowerCase(setter.charAt(3));
				property = c + setter.substring(4);
			}
			PropertyChangeSupport subscribers = beans.get(resource);
			PropertyChangeEvent evt = new PropertyChangeEvent(bean, property,
					null, newValue);
			subscribers.firePropertyChange(evt);
		}
	}

	NotifyingRepositoryConnection findBroadcaster(RepositoryConnection conn) {
		try {
			if (conn instanceof NotifyingRepositoryConnectionWrapper) {
				return (NotifyingRepositoryConnection) conn;
			} else if (conn instanceof DelegatingRepositoryConnection) {
				DelegatingRepositoryConnection dconn = (DelegatingRepositoryConnection) conn;
				return findBroadcaster(dconn.getDelegate());
			} else {
				return null;
			}
		} catch (StoreException e) {
			throw new AssertionError(e);
		}
	}
}
