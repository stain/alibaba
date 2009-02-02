package org.openrdf.script.evaluation;

import org.openrdf.cursor.Cursor;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.federation.evaluation.RepositoryTripleSource;
import org.openrdf.store.StoreException;

public class ScriptEvaluator extends EvaluationStrategyImpl {

	private RepositoryConnection con;

	public ScriptEvaluator(RepositoryConnection con, QueryModel query) {
		super(new RepositoryTripleSource(con), query);
		this.con = con;
	}

	public RepositoryConnection getConnection() {
		return con;
	}

	@Override
	public Cursor<BindingSet> evaluate(TupleExpr expr, BindingSet bindings)
			throws StoreException {
		// TODO Auto-generated method stub
		return super.evaluate(expr, bindings);
	}

}
