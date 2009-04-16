package org.openrdf.sail.federation.evaluation;

import java.util.Set;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.iterator.FilterIterator;

public class FilterCursor extends FilterIterator {

	public FilterCursor(
			CloseableIteration<BindingSet, QueryEvaluationException> result,
			ValueExpr condition, final Set<String> scopeBindingNames,
			EvaluationStrategy strategy) throws QueryEvaluationException {
		super(new Filter(new EmptySet() {

			@Override
			public Set<String> getBindingNames() {
				return scopeBindingNames;
			}
		}, condition), result, strategy);
	}

}
