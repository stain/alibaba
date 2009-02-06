package org.openrdf.script.model;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.query.algebra.NaryOperator;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.QueryModelVisitor;

public class ControlNode extends NaryOperator<QueryModelNode> {
	private static final long serialVersionUID = -4167628273586438039L;
	private List<URI> uris = new ArrayList<URI>();

	public ControlNode(URI uri, QueryModelNode target) {
		uris.add(uri);
		addArg(target);
	}

	public void addParameter(URI uri, QueryModelNode expr) {
		uris.add(uri);
		addArg(expr);
	}

	public QueryModelNode getParameter(URI uri) {
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
