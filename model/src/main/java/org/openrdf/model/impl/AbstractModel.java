package org.openrdf.model.impl;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.ModelException;
import org.openrdf.model.util.ModelUtil;

public abstract class AbstractModel extends AbstractSet<Statement> implements
		Model {
	private static final long serialVersionUID = 4254119331281455614L;

	@Override
	public boolean add(Statement st) {
		return add(st.getSubject(), st.getPredicate(), st.getObject(),
				st.getContext());
	}

	@Override
	public void clear() {
		remove(null, null, null);
	}

	public boolean clear(Resource... contexts) {
		return remove(null, null, null, contexts);
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Statement) {
			Statement st = (Statement) o;
			return remove(st.getSubject(), st.getPredicate(), st.getObject(),
					st.getContext());
		}
		return false;
	}

	public Value objectValue() throws ModelException {
		Iterator<Value> iter = objects().iterator();
		if (iter.hasNext()) {
			Value obj = iter.next();
			if (iter.hasNext()) {
				throw new ModelException(obj, iter.next());
			}
			return obj;
		}
		return null;
	}

	public Literal objectLiteral() throws ModelException {
		Value obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof Literal) {
			return (Literal) obj;
		}
		throw new ModelException(obj);
	}

	public Resource objectResource() throws ModelException {
		Value obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof Resource) {
			return (Resource) obj;
		}
		throw new ModelException(obj);
	}

	public URI objectURI() throws ModelException {
		Value obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof URI) {
			return (URI) obj;
		}
		throw new ModelException(obj);
	}

	public String objectString() throws ModelException {
		Value obj = objectValue();
		if (obj == null) {
			return null;
		}
		return obj.stringValue();
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Model) {
			Model model = (Model) o;
			return ModelUtil.equals(this, model);
		}
		return false;
	}

	public Set<Resource> subjects() {
		return new ValueSet<Resource>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Resource) {
					return AbstractModel.this
							.contains((Resource) o, null, null);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Resource) {
					return AbstractModel.this.remove((Resource) o, null, null);
				}
				return false;
			}

			@Override
			public boolean add(Resource subj) {
				return AbstractModel.this.add(subj, null, null);
			}

			@Override
			protected Resource term(Statement st) {
				return st.getSubject();
			}

			@Override
			protected void removeIteration(Iterator<Statement> iter,
					Resource subj) {
				AbstractModel.this.removeIteration(iter, subj, null, null);
			}
		};
	}

	public Set<URI> predicates() {
		return new ValueSet<URI>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof URI) {
					return AbstractModel.this.contains(null, (URI) o, null);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof URI) {
					return AbstractModel.this.remove(null, (URI) o, null);
				}
				return false;
			}

			@Override
			public boolean add(URI pred) {
				return AbstractModel.this.add(null, pred, null);
			}

			@Override
			protected URI term(Statement st) {
				return st.getPredicate();
			}

			@Override
			protected void removeIteration(Iterator<Statement> iter, URI pred) {
				AbstractModel.this.removeIteration(iter, null, pred, null);
			}
		};
	}

	public Set<Value> objects() {
		return new ValueSet<Value>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Value) {
					return AbstractModel.this.contains(null, null, (Value) o);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Value) {
					return AbstractModel.this.remove(null, null, (Value) o);
				}
				return false;
			}

			@Override
			public boolean add(Value obj) {
				return AbstractModel.this.add(null, null, obj);
			}

			@Override
			protected Value term(Statement st) {
				return st.getObject();
			}

			@Override
			protected void removeIteration(Iterator<Statement> iter, Value obj) {
				AbstractModel.this.removeIteration(iter, null, null, obj);
			}
		};
	}

	public Set<Resource> contexts() {
		return new ValueSet<Resource>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Resource || o == null) {
					return AbstractModel.this.contains(null, null, null,
							(Resource) o);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Resource || o == null) {
					return AbstractModel.this.remove(null, null, null,
							(Resource) o);
				}
				return false;
			}

			@Override
			public boolean add(Resource context) {
				return AbstractModel.this.add(null, null, null, context);
			}

			@Override
			protected Resource term(Statement st) {
				return st.getContext();
			}

			@Override
			protected void removeIteration(Iterator<Statement> iter,
					Resource term) {
				AbstractModel.this
						.removeIteration(iter, null, null, null, term);
			}
		};
	}

	private abstract class ValueSet<V extends Value> extends AbstractSet<V> {

		@Override
		public Iterator<V> iterator() {
			final Set<V> set = new LinkedHashSet<V>();
			final Iterator<Statement> iter = AbstractModel.this.iterator();
			return new Iterator<V>() {

				private Statement current;

				private Statement next;

				public boolean hasNext() {
					if (next == null) {
						next = findNext();
					}
					return next != null;
				}

				public V next() {
					if (next == null) {
						next = findNext();
						if (next == null) {
							throw new NoSuchElementException();
						}
					}
					current = next;
					next = null;
					V value = term(current);
					set.add(value);
					return value;
				}

				public void remove() {
					if (current == null) {
						throw new IllegalStateException();
					}
					removeIteration(iter, term(current));
					current = null;
				}

				private Statement findNext() {
					while (iter.hasNext()) {
						Statement st = iter.next();
						if (accept(st)) {
							return st;
						}
					}
					return null;
				}

				private boolean accept(Statement st) {
					return !set.contains(term(st));
				}
			};
		}

		@Override
		public abstract boolean add(V term);

		@Override
		public void clear() {
			AbstractModel.this.clear();
		}

		@Override
		public boolean isEmpty() {
			return AbstractModel.this.isEmpty();
		}

		@Override
		public int size() {
			Set<V> set = new LinkedHashSet<V>();
			Iterator<Statement> iter = AbstractModel.this.iterator();
			while (iter.hasNext()) {
				set.add(term(iter.next()));
			}
			return set.size();
		}

		protected abstract V term(Statement st);

		protected abstract void removeIteration(Iterator<Statement> iter, V term);
	}

	protected abstract void removeIteration(Iterator<Statement> iter,
			Resource subj, URI pred, Value obj, Resource... contexts);

}
