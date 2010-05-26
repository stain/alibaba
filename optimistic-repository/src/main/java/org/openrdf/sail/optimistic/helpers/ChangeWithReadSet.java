package org.openrdf.sail.optimistic.helpers;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;

public class ChangeWithReadSet {
	private Model added;
	private Model removed;
	private Set<EvaluateOperation> read = new HashSet<EvaluateOperation>();

	public ChangeWithReadSet(Model added, Model removed) {
		this.added = new LinkedHashModel(added);
		this.removed = new LinkedHashModel(removed);
	}

	public Model getAdded() {
		return added;
	}

	public Model getRemoved() {
		return removed;
	}

	public Set<EvaluateOperation> getReadOperations() {
		return read;
	}

	public void addRead(EvaluateOperation op) {
		read.add(op);
	}
}
