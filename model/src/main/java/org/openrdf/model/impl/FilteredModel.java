package org.openrdf.model.impl;

import java.util.Iterator;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

abstract class FilteredModel extends AbstractModel {
	private final Model model;

	private static final long serialVersionUID = -2353344619836326934L;

	private Resource subj;

	private URI pred;

	private Value obj;

	private Resource[] contexts;

	public FilteredModel(Model model, Resource subj, URI pred, Value obj,
			Resource... contexts) {
		this.model = model;
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.contexts = notNull(contexts);
	}

	public String getNamespace(String prefix) {
		return model.getNamespace(prefix);
	}

	public Map<String, String> getNamespaces() {
		return model.getNamespaces();
	}

	public String setNamespace(String prefix, String name) {
		return model.setNamespace(prefix, name);
	}

	public String removeNamespace(String prefix) {
		return model.removeNamespace(prefix);
	}

	@Override
	public int size() {
		int size = 0;
		Iterator<Statement> iter = iterator();
		while (iter.hasNext()) {
			size++;
			iter.next();
		}
		return size;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Statement) {
			Statement st = (Statement) o;
			if (accept(st)) {
				return model.contains(o);
			}
		}
		return false;
	}

	public boolean add(Resource s, URI p, Value o, Resource... c) {
		if (s == null) {
			s = subj;
		}
		if (p == null) {
			p = pred;
		}
		if (o == null) {
			o = obj;
		}
		if (c != null && c.length == 0) {
			c = contexts;
		}
		if (!accept(s, p, o, c)) {
			throw new IllegalArgumentException(
					"Statement is filtered out of view");
		}
		return model.add(s, p, o, c);
	}

	@Override
	public void clear() {
		model.remove(subj, pred, obj, contexts);
	}

	public boolean remove(Resource s, URI p, Value o, Resource... c) {
		if (s == null) {
			s = subj;
		}
		if (p == null) {
			p = pred;
		}
		if (o == null) {
			o = obj;
		}
		if (c != null && c.length == 0) {
			c = contexts;
		}
		if (!accept(s, p, o, c)) {
			return false;
		}
		return model.remove(s, p, o, c);
	}

	public boolean contains(Resource s, URI p, Value o, Resource... c) {
		if (s == null) {
			s = subj;
		}
		if (p == null) {
			p = pred;
		}
		if (o == null) {
			o = obj;
		}
		if (c != null && c.length == 0) {
			c = contexts;
		}
		if (!accept(s, p, o, c)) {
			return false;
		}
		return model.contains(s, p, o, c);
	}

	public Model filter(Resource s, URI p, Value o, Resource... c) {
		if (s == null) {
			s = subj;
		}
		if (p == null) {
			p = pred;
		}
		if (o == null) {
			o = obj;
		}
		if (c != null && c.length == 0) {
			c = contexts;
		}
		if (!accept(s, p, o, c)) {
			return new EmptyModel(getNamespaces());
		}
		return model.filter(s, p, o, c);
	}

	@Override
	protected void removeIteration(Iterator<Statement> iter, Resource s, URI p,
			Value o, Resource... c) {
		if (s == null) {
			s = subj;
		}
		if (p == null) {
			p = pred;
		}
		if (o == null) {
			o = obj;
		}
		if (c != null && c.length == 0) {
			c = contexts;
		}
		if (!accept(s, p, o, c)) {
			throw new IllegalStateException();
		}
		removeFilteredIteration(iter, s, p, o, c);
	}

	protected abstract void removeFilteredIteration(Iterator<Statement> iter,
			Resource s, URI p, Value o, Resource... c);

	private boolean accept(Statement st) {
		return matches(st, subj, pred, obj, contexts);
	}

	private boolean accept(Resource s, URI p, Value o, Resource... c) {
		if (subj != null && !subj.equals(s)) {
			return false;
		}
		if (pred != null && !pred.equals(p)) {
			return false;
		}
		if (obj != null && !obj.equals(o)) {
			return false;
		}
		if (!matches(notNull(c), contexts)) {
			return false;
		}
		return true;
	}

	private boolean matches(Statement st, Resource subj, URI pred, Value obj,
			Resource... contexts) {
		if (subj != null && !subj.equals(st.getSubject())) {
			return false;
		}
		if (pred != null && !pred.equals(st.getPredicate())) {
			return false;
		}
		if (obj != null && !obj.equals(st.getObject())) {
			return false;
		}

		return matches(st.getContext(), contexts);
	}

	private boolean matches(Resource[] stContext, Resource... contexts) {
		if (stContext != null && stContext.length > 0) {
			for (Resource c : stContext) {
				if (!matches(c, contexts)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean matches(Resource stContext, Resource... contexts) {
		if (contexts != null && contexts.length == 0) {
			// Any context matches
			return true;
		} else {
			// Accept if one of the contexts from the pattern matches
			for (Resource context : notNull(contexts)) {
				if (context == null && stContext == null) {
					return true;
				}
				if (context != null && context.equals(stContext)) {
					return true;
				}
			}

			return false;
		}
	}

	private Resource[] notNull(Resource[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
	}
}