package org.openrdf.model.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class EmptyModel extends AbstractModel {
	private Map<String, String> namespaces;

	public EmptyModel() {
		this(new HashMap<String, String>());
	}

	public EmptyModel(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}

	private static final long serialVersionUID = 3123007631452759092L;

	private Set<Statement> emptySet = Collections.emptySet();

	public String getNamespace(String prefix) {
		return this.namespaces.get(prefix);
	}

	public Map<String, String> getNamespaces() {
		return this.namespaces;
	}

	public String setNamespace(String prefix, String name) {
		return this.namespaces.put(prefix, name);
	}

	public String removeNamespace(String prefix) {
		return this.namespaces.remove(prefix);
	}

	@Override
	public Iterator<Statement> iterator() {
		return emptySet.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException("All statements are filtered out of view");
	}

	public boolean contains(Resource subj, URI pred, Value obj, Resource... contexts) {
		return false;
	}

	public Model filter(Resource subj, URI pred, Value obj, Resource... contexts) {
		return this;
	}

	public boolean remove(Resource subj, URI pred, Value obj, Resource... contexts) {
		return false;
	}

	protected void removeIteration(Iterator<Statement> iter, Resource subj,
			URI pred, Value obj, Resource... contexts) {
		// remove nothing
	}

}