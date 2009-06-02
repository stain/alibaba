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

import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Finds nodes that do not filter their values.
 * 
 * @author James Leigh
 * 
 */
public class BasicNodeCollector extends QueryModelVisitorBase<RuntimeException> {
	private List<TupleExpr> list;
	private TupleExpr root;

	public BasicNodeCollector(TupleExpr root) {
		this.root = root;
	}

	public List<TupleExpr> findBasicNodes() {
		if (isBasic(root))
			return Collections.singletonList(root);
		list = new ArrayList<TupleExpr>();
		root.visit(this);
		return list;
	}

	@Override
	public void meet(StatementPattern node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		}
	}

	@Override
	protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		} else {
			super.meetBinaryTupleOperator(node);
		}
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
		if (!isBasic(node.getParentNode()) && isBasic(node)) {
			list.add(node);
		} else {
			super.meetUnaryTupleOperator(node);
		}
	}

	private boolean isBasic(QueryModelNode node) {
		return node instanceof TupleExpr && new BasicNodeJudge(node).isBasic();
	}

}
