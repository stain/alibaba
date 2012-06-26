package org.openrdf.sail.auditing.helpers;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.algebra.Var;

public class OperationEntityResolver {
	private final ValueFactory vf;
	private UpdateExpr updateExpr;
	private Dataset dataset;
	private BindingSet bindings;
	private URI entity;

	public OperationEntityResolver(ValueFactory vf) {
		this.vf = vf;
	}

	public synchronized URI getEntity(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings) {
		if (dataset == null)
			return null;
		URI activity = dataset.getDefaultInsertGraph();
		if (activity == null || activity.stringValue().indexOf('#') >= 0)
			return null;
		if (this.updateExpr == updateExpr && this.dataset == dataset && this.bindings == bindings)
			return this.entity;
		final Set<Var> subjects = new HashSet<Var>();
		final Set<Var> objects = new HashSet<Var>();
		try {
			updateExpr.visit(new BasicGraphPatternVisitor() {
				public void meet(StatementPattern node) {
					subjects.add(node.getSubjectVar());
					objects.add(node.getObjectVar());
				}
			});
		} catch (QueryEvaluationException e) {
			throw new AssertionError(e);
		}
		URI entity = null;
		for (Var var : subjects) {
			Value subj = var.getValue();
			if (subj == null) {
				subj = bindings.getValue(var.getName());
			}
			if (subj instanceof URI) {
				if (entity == null) {
					entity = entity((URI) subj);
				} else if (!entity.equals(entity((URI) subj))) {
					return null;
				}
			} else if (!objects.contains(var)) {
				return null;
			}
		}
		this.updateExpr = updateExpr;
		this.dataset = dataset;
		this.bindings = bindings;
		this.entity = entity;
		return entity;
	}

	private URI entity(URI subject) {
		URI entity = subject;
		int hash = entity.stringValue().indexOf('#');
		if (hash > 0) {
			entity = vf.createURI(entity.stringValue().substring(0, hash));
		}
		return entity;
	}
}
