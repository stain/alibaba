package org.openrdf.alibaba.core.base;

import java.util.Iterator;
import java.util.Set;

import org.openrdf.alibaba.core.RepositoryBehaviour;

public class RepositoryBase<E> implements RepositoryBehaviour<E> {
	private Set<E> set;

	protected RepositoryBase(Set<E> set) {
		this.set = set;
	}

	public boolean add(E o) {
		return set.add(o);
	}

	public boolean contains(E o) {
		return set.contains(o);
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public Iterator<E> iterator() {
		return set.iterator();
	}

	public boolean remove(E o) {
		return set.remove(o);
	}

	public int size() {
		return set.size();
	}
}
