package org.openrdf.sail.optimistic.helpers;

import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Statement;
import org.openrdf.query.QueryEvaluationException;

public class NamedContextCursor extends FilterIteration<Statement, QueryEvaluationException> {

	public NamedContextCursor(
			Iteration<? extends Statement, ? extends QueryEvaluationException> iter) {
		super(iter);
	}

	@Override
	protected boolean accept(Statement st) {
		return st.getContext() != null;
	}

}
