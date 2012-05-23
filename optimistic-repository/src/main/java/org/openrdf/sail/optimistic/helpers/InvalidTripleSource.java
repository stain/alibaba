package org.openrdf.sail.optimistic.helpers;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.evaluation.TripleSource;

public class InvalidTripleSource implements TripleSource {
	private final ValueFactory vf;

	public InvalidTripleSource(ValueFactory vf) {
		this.vf = vf;
	}

	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
			Resource subj, URI pred, Value obj, Resource... contexts)
			throws QueryEvaluationException {
		throw new QueryEvaluationException("Store cannot be accessed from here");
	}

	public ValueFactory getValueFactory() {
		return vf;
	}

}
