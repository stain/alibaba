package org.openrdf.script;

import java.util.Map;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.script.ast.ParseException;
import org.openrdf.script.ast.TokenMgrError;
import org.openrdf.script.evaluation.ScriptEvaluator;
import org.openrdf.script.model.Body;
import org.openrdf.store.StoreException;

public class ScriptEngine {

	private ObjectRepository repository;

	public ScriptEngine(ObjectRepository repository) {
		this.repository = repository;
	}

	public Value evalSingleValue(String code) throws StoreException,
			TokenMgrError, ParseException {
		return evalSingleValue(code, null, null);
	}

	public Value evalSingleValue(String code, String base,
			Map<String, String> namespaces) throws TokenMgrError,
			ParseException, StoreException {
		ObjectConnection con = repository.getConnection();
		try {
			ScriptParser parser = new ScriptParser();
			ScriptEvaluator eval = new ScriptEvaluator(con, true);
			Body body = parser.parse(code, base, namespaces);
			BindingSet bindings = new QueryBindingSet();
			Value result = null;
			for (QueryModelNode node : body.getArgs()) {
				// TODO node instanceof ValueExpr
				result = eval.evaluate((ValueExpr) node, bindings);
			}
			return result;
		} finally {
			con.close();
		}
	}

}
