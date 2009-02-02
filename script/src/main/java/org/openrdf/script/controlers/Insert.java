package org.openrdf.script.controlers;

import static org.openrdf.sail.federation.query.QueryModelSerializer.LANGUAGE;

import java.util.Map;

import org.openrdf.cursor.CollectionCursor;
import org.openrdf.cursor.ConvertingCursor;
import org.openrdf.cursor.Cursor;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.sail.federation.query.QueryModelSerializer;
import org.openrdf.script.Control;
import org.openrdf.script.evaluation.ScriptEvaluator;
import org.openrdf.script.helpers.ModelInserter;
import org.openrdf.script.vocabulary.Script;
import org.openrdf.store.StoreException;

public class Insert implements Control {

	public String getURI() {
		return Script.NAMESPACE+"insert";
	}

	public Cursor<BindingSet> evaluate(ScriptEvaluator eval, TupleExpr target,
			BindingSet bindings, Map<URI, TupleExpr> args) throws StoreException {
		QueryModelSerializer serializer = new QueryModelSerializer();
		QueryModel model = new QueryModel(target);
		String qry = serializer.writeQueryModel(model, null);
		RepositoryConnection con = eval.getConnection();
		GraphQuery query = con.prepareGraphQuery(LANGUAGE, qry);
		ModelInserter inserter = new ModelInserter(con);
		try {
			query.evaluate(inserter);
		} catch (RDFHandlerException e) {
			throw (StoreException) e.getCause();
		}
		return evaluate(inserter.getModel(), bindings);
	}

	private Cursor<BindingSet> evaluate(Model model, final BindingSet bindings) throws StoreException {
		Cursor<Statement> cursor = new CollectionCursor<Statement>(model);
		return new ConvertingCursor<Statement, BindingSet>(cursor) {

			@Override
			protected BindingSet convert(Statement st) throws StoreException {
				QueryBindingSet set = new QueryBindingSet(bindings);
				set.setBinding("subject", st.getSubject());
				set.setBinding("predicate", st.getPredicate());
				set.setBinding("object", st.getObject());
				set.setBinding("context", st.getContext());
				return set;
			}
		};
	}

}
