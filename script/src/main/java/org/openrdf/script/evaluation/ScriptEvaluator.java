package org.openrdf.script.evaluation;

import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.repository.RepositoryConnection;

public class ScriptEvaluator extends EvaluationStrategyImpl {

	private RepositoryConnection con;

	public ScriptEvaluator(RepositoryConnection con, boolean inf) {
		super(new RepositoryTripleSource(con, inf));
		this.con = con;
	}

	public RepositoryConnection getConnection() {
		return con;
	}

}
