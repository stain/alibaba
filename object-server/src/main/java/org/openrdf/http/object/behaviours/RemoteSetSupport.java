package org.openrdf.http.object.behaviours;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.BehaviourException;

public class RemoteSetSupport implements Set {
	private String uri;
	private String qs;
	private Type gtype;
	private Set values;
	private ObjectConnection oc;

	public RemoteSetSupport(String uri, String qs, Type gtype, Set values,
			ObjectConnection oc) {
		this.uri = uri;
		this.qs = qs;
		this.gtype = gtype;
		this.values = values;
		this.oc = oc;
	}

	public boolean add(Object e) {
		boolean changed = values.add(e);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean addAll(Collection c) {
		boolean changed = values.addAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean remove(Object o) {
		boolean changed = values.remove(o);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean removeAll(Collection c) {
		boolean changed = values.removeAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public boolean retainAll(Collection c) {
		boolean changed = values.retainAll(c);
		if (changed) {
			store(values);
		}
		return changed;
	}

	public void clear() {
		try {
			RemoteConnection con = openConnection("DELETE");
			int status = con.getResponseCode();
			if (status >= 300) {
				String msg = con.getResponseMessage();
				String stack = con.readString();
				throw ResponseException.create(status, msg, stack);
			}
			values.clear();
		} catch (IOException e) {
			throw new BehaviourException(e);
		}
	}

	public Iterator iterator() {
		final Iterator delegate = values.iterator();
		return new Iterator() {

			public boolean hasNext() {
				return delegate.hasNext();
			}

			public Object next() {
				return delegate.next();
			}

			public void remove() {
				delegate.remove();
				store(values);
			}
		};
	}

	public boolean contains(Object o) {
		return values.contains(o);
	}

	public boolean containsAll(Collection c) {
		return values.containsAll(c);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public int size() {
		return values.size();
	}

	public Object[] toArray() {
		return values.toArray();
	}

	public Object[] toArray(Object[] a) {
		return values.toArray(a);
	}

	@Override
	public boolean equals(Object obj) {
		return values.equals(obj);
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}

	@Override
	public String toString() {
		return values.toString();
	}

	private void store(Set values) {
		try {
			RemoteConnection con = openConnection("PUT");
			con.write(null, Set.class, gtype, values);
			int status = con.getResponseCode();
			if (status >= 300) {
				String msg = con.getResponseMessage();
				String stack = con.readString();
				throw ResponseException.create(status, msg, stack);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BehaviourException(e);
		}
	}

	private RemoteConnection openConnection(String method) throws IOException {
		return new RemoteConnection(method, uri, qs, oc);
	}

}
