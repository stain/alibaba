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

import java.util.Collection;
import java.util.LinkedList;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.MemoryOverflowModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.Difference;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

/**
 * Move delta changes into the query model.
 * 
 * @author James Leigh
 * 
 */
public class DeltaMerger extends QueryModelVisitorBase<RuntimeException>
		implements QueryOptimizer {
	private final Model added;
	private final Model removed;
	private Dataset dataset;
	private BindingSet bindings;
	private BindingSet additional;
	private boolean modified;
	private int varCount;
	private final Collection<MemoryOverflowModel> open;

	public DeltaMerger(Model added, Model removed) {
		this.added = added;
		this.removed = removed;
		this.open = new LinkedList<MemoryOverflowModel>();
	}

	public DeltaMerger(Model added,
			BindingSet additional) {
		this.added = added;
		this.removed = new LinkedHashModel();
		this.additional = additional;
		this.open = null;
	}

	public synchronized void close() {
		if (open != null) {
			for (MemoryOverflowModel model : open) {
				model.release();
			}
			open.clear();
		}
	}

	public boolean isModified() {
		return modified;
	}

	public void optimize(TupleExpr query, Dataset dataset,
			BindingSet bindings) {
		this.dataset = dataset;
		this.modified = false;
		this.bindings = bindings;
		query.visit(this);
	}

	@Override
	public void meet(StatementPattern sp) throws RuntimeException {
		super.meet(sp);
		ExternalModel externalR = new ExternalModel(sp, dataset, additional);
		ExternalModel externalA = new ExternalModel(sp, dataset, additional);

		Model minus = open(externalR.filter(removed, bindings));
		Model union = open(externalA.filter(added, bindings));

		TupleExpr node = sp;
		if (!minus.isEmpty()) {
			modified = true;
			externalR.setModel(minus);
			if (sp.getContextVar() == null) {
				// difference must compare context, but only works if non-null
				sp.setContextVar(newVar());
			}
			Difference rpl = new Difference(node.clone(), externalR);
			node.replaceWith(rpl);
			node = rpl;
		}
		if (!union.isEmpty()) {
			modified = true;
			externalA.setModel(union);
			Union rpl = new Union(externalA, node.clone());
			node.replaceWith(rpl);
			node = rpl;
		}
	}

	private synchronized Model open(Model filtered) {
		if (open == null)
			return filtered;
		MemoryOverflowModel model = new MemoryOverflowModel(filtered);
		if (model.isEmpty()) {
			model.release();
			return new LinkedHashModel();
		} else {
			open.add(model);
			return model;
		}
	}

	private synchronized Var newVar() {
		Var var = new Var("-delta-merger-graph-" + (varCount++));
		var.setAnonymous(true);
		return var;
	}

}
