package org.openrdf.sail.optimistic.helpers;

import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

public class StatementPatternCursor extends FilterIteration<Statement, QueryEvaluationException> {

	private Var subjVar;
	private Var predVar;
	private Var objVar;
	private Var conVar;

	public StatementPatternCursor(
			Iteration<? extends Statement, ? extends QueryEvaluationException> iter, StatementPattern sp) {
		super(iter);
		subjVar = sp.getSubjectVar();
		predVar = sp.getPredicateVar();
		objVar = sp.getObjectVar();
		conVar = sp.getContextVar();
	}

	@Override
	protected boolean accept(Statement st) {
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource context = st.getContext();

		if (subjVar != null) {
			if (subjVar.equals(predVar) && !subj.equals(pred)) {
				return false;
			}
			if (subjVar.equals(objVar) && !subj.equals(obj)) {
				return false;
			}
			if (subjVar.equals(conVar) && !subj.equals(context)) {
				return false;
			}
		}

		if (predVar != null) {
			if (predVar.equals(objVar) && !pred.equals(obj)) {
				return false;
			}
			if (predVar.equals(conVar) && !pred.equals(context)) {
				return false;
			}
		}

		if (objVar != null) {
			if (objVar.equals(conVar) && !obj.equals(context)) {
				return false;
			}
		}

		return true;
	}
}