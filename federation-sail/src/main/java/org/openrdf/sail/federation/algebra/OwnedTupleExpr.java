/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.federation.algebra;

import info.aduna.iteration.CloseableIteration;

import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.QueryModelVisitor;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.federation.evaluation.InsertBindingSetCursor;

/**
 * Indicates that the argument should be evaluated in a particular member.
 * 
 * @author James Leigh
 */
public class OwnedTupleExpr extends UnaryTupleOperator {

	private RepositoryConnection owner;

	private TupleQuery query;

	private Set<String> bindingNames;

	public OwnedTupleExpr(RepositoryConnection owner, TupleExpr arg) {
		super(arg);
		this.owner = owner;
		this.bindingNames = arg.getBindingNames();
	}

	public RepositoryConnection getOwner() {
		return owner;
	}

	@Override
	public Set<String> getBindingNames() {
		return bindingNames;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			Dataset dataset, BindingSet bindings)
			throws QueryEvaluationException {
		if (query == null) {
			return null;
		}
		synchronized (query) {
			for (String name : bindings.getBindingNames()) {
				if (bindingNames.contains(name)) {
					query.setBinding(name, bindings.getValue(name));
				}
			}
			query.setDataset(dataset);
			TupleQueryResult result = query.evaluate();
			return new InsertBindingSetCursor(result, bindings);
		}
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public String getSignature() {
		return this.getClass().getSimpleName() + " " + owner.toString();
	}

}
