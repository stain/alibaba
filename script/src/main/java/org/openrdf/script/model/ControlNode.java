package org.openrdf.script.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.query.algebra.NaryTupleOperator;
import org.openrdf.query.algebra.QueryModelVisitor;
import org.openrdf.query.algebra.TupleExpr;

public class ControlNode extends NaryTupleOperator {
	private static final long serialVersionUID = -4167628273586438039L;
	private List<URI> uris = new ArrayList<URI>();

	public ControlNode(URI uri, TupleExpr target) {
		uris.add(uri);
		addArg(target);
	}

	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);

		for (TupleExpr arg : getArgs()) {
			bindingNames.addAll(arg.getBindingNames());
		}

		return bindingNames;
	}

	public void addParameter(URI uri, TupleExpr expr) {
		uris.add(uri);
		addArg(expr);
	}

	public TupleExpr getParameter(URI uri) {
		int idx = uris.indexOf(uri);
		if (idx < 0)
			return null;
		return getArg(idx);
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

}
