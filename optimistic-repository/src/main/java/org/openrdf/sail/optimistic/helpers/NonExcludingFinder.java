/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.sail.optimistic.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrdf.query.algebra.Add;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Clear;
import org.openrdf.query.algebra.Copy;
import org.openrdf.query.algebra.Create;
import org.openrdf.query.algebra.DeleteData;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.Load;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.Move;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Finds nodes that do not exclude their binding values.
 * 
 * @author James Leigh
 * 
 */
public class NonExcludingFinder extends QueryModelVisitorBase<RuntimeException> {
	private List<TupleExpr> list;
	private int varCount;

	public synchronized List<TupleExpr> find(QueryModelNode root) {
		if (!isExclusionPresent(root))
			return Collections.singletonList((TupleExpr) root);
		if (new InlineFilterExists().isPresent(root)) {
			root = root.clone();
			if (root instanceof TupleExpr) {
				new InlineFilterExists().optimize((TupleExpr) root, null, null);
			} else if (root instanceof Modify) {
				TupleExpr where = ((Modify) root).getWhereExpr();
				new InlineFilterExists().optimize(where, null, null);
			}
		}
		varCount = 0;
		list = new ArrayList<TupleExpr>();
		root.visit(this);
		return list;
	}

	@Override
	public void meet(StatementPattern node) {
		if (isExclusionPresent(node.getParentNode()) && !isExclusionPresent(node)) {
			list.add(node);
		}
	}

	@Override
	public void meet(Add node) throws RuntimeException {
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getSourceGraph())));
	}

	@Override
	public void meet(Clear node) throws RuntimeException {
		if (node.getGraph() != null) {
			list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getGraph())));
		} else {
			list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar()));
		}
	}

	@Override
	public void meet(Copy node) throws RuntimeException {
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getSourceGraph())));
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getDestinationGraph())));
	}

	@Override
	public void meet(Create node) throws RuntimeException {
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getGraph())));
	}

	@Override
	public void meet(DeleteData node) throws RuntimeException {
		// no effect
	}

	@Override
	public void meet(InsertData node) throws RuntimeException {
		// no effect
	}

	@Override
	public void meet(Load node) throws RuntimeException {
		// no effect
	}

	@Override
	public void meet(Modify node) throws RuntimeException {
		node.getWhereExpr().visit(this);
	}

	@Override
	public void meet(Move node) throws RuntimeException {
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getSourceGraph())));
		list.add(new StatementPattern(newVar(), newVar(), newVar(), newVar(node.getDestinationGraph())));
	}

	@Override
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
		if (isExclusionPresent(node.getParentNode()) && !isExclusionPresent(node)) {
			list.add(node);
		} else {
			super.meetBinaryTupleOperator(node);
		}
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
		if (isExclusionPresent(node.getParentNode()) && !isExclusionPresent(node)) {
			list.add(node);
		} else {
			super.meetUnaryTupleOperator(node);
		}
	}

	private Var newVar(ValueConstant value) {
		return new Var("-static-" + (varCount++), value.getValue());
	}

	private synchronized Var newVar() {
		Var var = new Var("-wild-" + (varCount++));
		var.setAnonymous(true);
		return var;
	}

	private boolean isExclusionPresent(QueryModelNode node) {
		return !(node instanceof TupleExpr) || new ExclusionDetector(node).isExclusionPresent();
	}

}
