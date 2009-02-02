package org.openrdf.sail.optimistic.helpers;

import java.util.Set;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.Cursor;
import org.openrdf.cursor.EmptyCursor;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.algebra.evaluation.cursors.NamedContextCursor;
import org.openrdf.query.algebra.evaluation.cursors.StatementBindingSetCursor;
import org.openrdf.query.algebra.evaluation.cursors.StatementPatternCursor;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;
import org.openrdf.store.StoreException;

public class ExternalModel extends ExternalSet {
	private static final long serialVersionUID = -6075593457635970093L;
	private StatementPattern sp;
	private Model model;
	private Dataset dataset;
	private BindingSet bindings;

	public ExternalModel(StatementPattern sp, Dataset dataset) {
		this.sp = sp;
		this.dataset = dataset;
	}

	public ExternalModel(StatementPattern sp, Dataset dataset, BindingSet bindings) {
		this(sp, dataset);
		this.bindings = bindings;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public ExternalModel clone() {
		return (ExternalModel)super.clone();
	}

	public Model filter(Model model, BindingSet bindings) {
		Resource subj = (Resource) value(sp.getSubjectVar(), bindings);
		URI pred = (URI) value(sp.getPredicateVar(), bindings);
		Value obj = value(sp.getObjectVar(), bindings);
		Resource[] contexts = contexts(sp, dataset, bindings);
		if (contexts == null)
			return new LinkedHashModel();

		return model.filter(subj, pred, obj, contexts);
	}

	@Override
	public Set<String> getBindingNames() {
		return sp.getBindingNames();
	}

	@Override
	public double cardinality() {
		return model.size();
	}

	@Override
	public Cursor<BindingSet> evaluate(BindingSet bindings)
			throws StoreException {
		if (this.bindings != null) {
			QueryBindingSet b = new QueryBindingSet(bindings);
			b.addAll(this.bindings);
			bindings = b;
		}
		Cursor<? extends Statement> stIter = null;
		try {
			Resource[] contexts = contexts(sp, dataset, bindings);
			if (contexts == null)
				return new EmptyCursor<BindingSet>();

			Model filtered = filter(model, bindings);
			stIter = new CollectionCursor<Statement>(filtered);

			if (contexts.length == 0 && sp.getScope() == Scope.NAMED_CONTEXTS) {
				stIter = new NamedContextCursor(stIter);
			}
		} catch (ClassCastException e) {
			// Invalid value type for subject, predicate and/or context
			return new EmptyCursor<BindingSet>();
		}

		// The same variable might have been used multiple times in this
		// StatementPattern, verify value equality in those cases.
		stIter = new StatementPatternCursor(stIter, sp);

		// Return an iterator that converts the statements to var bindings
		return new StatementBindingSetCursor(stIter, sp, bindings);
	}

	private Value value(Var var, BindingSet bindings) {
		if (var == null)
			return null;
		if (var.getValue() != null)
			return var.getValue();
		return bindings.getValue(var.getName());
	}

	private Resource[] contexts(StatementPattern sp, Dataset dataset,
			BindingSet bindings) {
		Value contextValue = value(sp.getContextVar(), bindings);
		if (dataset == null) {
			if (contextValue != null)
				return new Resource[] { (Resource) contextValue };
			return new Resource[0];
		}
		Set<URI> graphs;
		if (sp.getScope() == Scope.DEFAULT_CONTEXTS) {
			graphs = dataset.getDefaultGraphs();
		} else {
			graphs = dataset.getNamedGraphs();
		}

		if (graphs.isEmpty()) {
			// Search zero contexts
			return null;
		}
		if (contextValue == null)
			return graphs.toArray(new Resource[graphs.size()]);
		if (graphs.contains(contextValue)) {
			return new Resource[] { (Resource) contextValue };
		}
		// Statement pattern specifies a context that is not part of the dataset
		return null;
	}

}
