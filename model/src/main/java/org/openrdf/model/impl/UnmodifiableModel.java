package org.openrdf.model.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

class UnmodifiableModel extends AbstractModel {
	private static final long serialVersionUID = 6335569454318096059L;
	private final AbstractModel model;

	public UnmodifiableModel(AbstractModel delegate) {
		this.model = delegate;
	}

	public Map<String, String> getNamespaces() {
		return Collections.unmodifiableMap(model.getNamespaces());
	}

	public String getNamespace(String prefix) {
		return model.getNamespace(prefix);
	}

	public String setNamespace(String prefix, String name) {
		throw new UnsupportedOperationException();
	}

	public String removeNamespace(String prefix) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		return model.contains(subj, pred, obj, contexts);
	}

	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	public Model filter(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		return model.filter(subj, pred, obj, contexts).unmodifiable();
	}

	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableSet(model).iterator();
	}

	@Override
	public int size() {
		return model.size();
	}

	@Override
	protected void removeIteration(Iterator<Statement> iter, Resource subj,
			URI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

}
