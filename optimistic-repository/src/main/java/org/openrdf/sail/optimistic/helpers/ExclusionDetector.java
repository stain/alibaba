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

import org.openrdf.query.algebra.ArbitraryLengthPath;
import org.openrdf.query.algebra.BNodeGenerator;
import org.openrdf.query.algebra.BinaryValueOperator;
import org.openrdf.query.algebra.BindingSetAssignment;
import org.openrdf.query.algebra.Bound;
import org.openrdf.query.algebra.Coalesce;
import org.openrdf.query.algebra.Difference;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.FunctionCall;
import org.openrdf.query.algebra.If;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.SingletonSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UnaryValueOperator;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.ZeroLengthPath;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Identifies if a node contains any form of exclusion.
 * 
 * @author James Leigh
 *
 */
public class ExclusionDetector extends QueryModelVisitorBase<RuntimeException> {
	private QueryModelNode root;
	private boolean excludingNode;
	private boolean excluding;
	private boolean leftJoinPresent;

	public ExclusionDetector(QueryModelNode root) {
		this.root = root;
		leftJoinPresent = new LeftJoinDetector(root).isPresent();
	}

	public boolean isExclusionPresent() {
		boolean before = excluding;
		try {
			excluding = false;
			excludingNode = true;
			root.visit(this);
			return excluding;
		} finally {
			excluding = before;
		}
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(BindingSetAssignment node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(BNodeGenerator node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Bound node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Coalesce node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	/**
	 * Only basic if right side is static constant.
	 */
	@Override
	public void meet(Difference node) throws RuntimeException {
		excludingNode = !(node.getRightArg() instanceof ExternalSet);
		super.meet(node);
	}

	@Override
	public void meet(EmptySet node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Extension node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(ExtensionElem node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	/**
	 * Only basic if there is no OPTIONAL join.
	 */
	@Override
	public void meet(Filter node) throws RuntimeException {
		excludingNode = leftJoinPresent;
		super.meet(node);
	}

	@Override
	public void meet(FunctionCall node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(If node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	/**
	 * Only basic if there is no condition.
	 */
	@Override
	public void meet(LeftJoin node) throws RuntimeException {
		excludingNode = node.hasCondition();
		super.meet(node);
	}

	@Override
	public void meet(SingletonSet node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Union node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	public void meet(ZeroLengthPath node) throws RuntimeException {
		excludingNode = false;
		super.meet(node);
	}

	@Override
	protected void meetBinaryValueOperator(BinaryValueOperator node) {
		excludingNode = false;
		super.meetBinaryValueOperator(node);
	}

	@Override
	protected void meetNode(QueryModelNode node) throws RuntimeException {
		if (!excludingNode) {
			excludingNode = true;
			node.visitChildren(this);
		} else {
			excluding = true;
		}
	}

	@Override
	protected void meetUnaryValueOperator(UnaryValueOperator node) {
		excludingNode = false;
		super.meetUnaryValueOperator(node);
	}

}
