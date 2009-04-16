/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.federation.optimizers;

import java.lang.reflect.UndeclaredThrowableException;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.federation.algebra.OwnedTupleExpr;

/**
 * Remove redundent {@link OwnedTupleExpr}.
 * 
 * @author James Leigh
 */
public class PrepareOwnedTupleExpr extends
		QueryModelVisitorBase<RepositoryException> implements QueryOptimizer {

	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {
		try {
			query.visit(this);
		} catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public void meetOther(QueryModelNode node) throws RepositoryException {
		if (node instanceof OwnedTupleExpr) {
			meetOwnedTupleExpr((OwnedTupleExpr) node);
		} else {
			super.meetOther(node);
		}
	}

	private void meetOwnedTupleExpr(OwnedTupleExpr node)
			throws RepositoryException {
		if (isRemoteQueryModelSupported(node.getOwner(), node.getArg())) {
			node.prepare();
		}
	}

	private boolean isRemoteQueryModelSupported(RepositoryConnection owner,
			TupleExpr expr) throws RepositoryException {
		if (expr instanceof StatementPattern) {
			return false;
		}
		if (((TupleExpr) expr).getBindingNames().size() == 0) {
			return false;
		}
		if (expr instanceof ExternalSet) {
			return false;
		}
		return false;
	}

}
