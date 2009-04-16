package org.openrdf.sail.optimistic.helpers;

import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;

public class StatementBindingSetCursor extends
		ConvertingIteration<Statement, BindingSet, QueryEvaluationException> {

	private Var subjVar;
	private Var predVar;
	private Var objVar;
	private Var conVar;
	private BindingSet bindings;

	public StatementBindingSetCursor(
			Iteration<? extends Statement, ? extends QueryEvaluationException> iter,
			StatementPattern sp, BindingSet bindings) {
		super(iter);
		subjVar = sp.getSubjectVar();
		predVar = sp.getPredicateVar();
		objVar = sp.getObjectVar();
		conVar = sp.getContextVar();
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(Statement st) {
		QueryBindingSet result = new QueryBindingSet(bindings);

		if (subjVar != null && !result.hasBinding(subjVar.getName())) {
			result.addBinding(subjVar.getName(), st.getSubject());
		}
		if (predVar != null && !result.hasBinding(predVar.getName())) {
			result.addBinding(predVar.getName(), st.getPredicate());
		}
		if (objVar != null && !result.hasBinding(objVar.getName())) {
			result.addBinding(objVar.getName(), st.getObject());
		}
		if (conVar != null && !result.hasBinding(conVar.getName())
				&& st.getContext() != null) {
			result.addBinding(conVar.getName(), st.getContext());
		}

		return result;
	}
}