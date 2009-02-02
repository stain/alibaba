package org.openrdf.script;

import java.util.Map;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.script.evaluation.ScriptEvaluator;
import org.openrdf.store.StoreException;

public interface Control {

	String getURI();

	Cursor<BindingSet> evaluate(ScriptEvaluator eval, TupleExpr target,
			BindingSet bindings, Map<URI, TupleExpr> args)
			throws StoreException;

}
