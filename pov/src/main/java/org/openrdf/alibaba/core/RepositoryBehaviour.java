package org.openrdf.alibaba.core;

import java.util.Iterator;

/**
 * A base interface for Entities that are themselves collections.
 * 
 * @author James Leigh
 * 
 * @param <E>
 *            Element type
 */
public interface RepositoryBehaviour<E> extends Iterable<E> {

	public abstract boolean add(E o);

	public abstract boolean contains(E o);

	public abstract boolean isEmpty();

	public abstract Iterator<E> iterator();

	public abstract boolean remove(E o);

	public abstract int size();

}