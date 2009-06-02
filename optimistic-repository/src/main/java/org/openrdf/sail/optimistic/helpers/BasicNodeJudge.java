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

import org.openrdf.query.algebra.BinaryValueOperator;
import org.openrdf.query.algebra.Difference;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.SingletonSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UnaryValueOperator;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

public class BasicNodeJudge extends QueryModelVisitorBase<RuntimeException> {
	private QueryModelNode root;
	private boolean basic;
	private boolean complex;
	private boolean leftJoinPresent;

	public BasicNodeJudge(QueryModelNode root) {
		this.root = root;
		leftJoinPresent = new LeftJoinDetector(root).isPresent();
	}

	public boolean isBasic() {
		boolean before = complex;
		try {
			complex = false;
			root.visit(this);
			return !complex;
		} finally {
			complex = before;
		}
	}

	/**
	 * Only basic if right side is constant.
	 */
	@Override
	public void meet(Difference node) throws RuntimeException {
		basic = node.getRightArg() instanceof ExternalSet;
		super.meet(node);
	}

	@Override
	public void meet(EmptySet node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	/**
	 * Only basic if there is no OPTIONAL join.
	 */
	@Override
	public void meet(Filter node) throws RuntimeException {
		basic = !leftJoinPresent;
		super.meet(node);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	/**
	 * Only basic if there is no condition.
	 */
	@Override
	public void meet(LeftJoin node) throws RuntimeException {
		basic = !node.hasCondition();
		super.meet(node);
	}

	@Override
	public void meet(SingletonSet node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(Union node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		basic = true;
		super.meet(node);
	}

	@Override
	protected void meetBinaryValueOperator(BinaryValueOperator node) {
		basic = true;
		super.meetBinaryValueOperator(node);
	}

	@Override
	protected void meetUnaryValueOperator(UnaryValueOperator node) {
		basic = true;
		super.meetUnaryValueOperator(node);
	}

	@Override
	protected void meetNode(QueryModelNode node) throws RuntimeException {
		if (basic) {
			basic = false;
			node.visitChildren(this);
		} else {
			complex = true;
		}
	}

}
