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

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.EmptyIteration;

import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.StatementPattern.Scope;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.algebra.evaluation.impl.ExternalSet;

/**
 * When evaluated will return the contents of a {@link Model}.
 * 
 * @author James Leigh
 *
 */
public class ExternalModel extends ExternalSet {
	private static final long serialVersionUID = -6075593457635970093L;
	private StatementPattern sp;
	private Model model;
	private Dataset dataset;
	private BindingSet bindings;

	public ExternalModel(StatementPattern sp, Dataset dataset) {
		this.sp = sp;
		this.dataset = dataset;
	}

	public ExternalModel(StatementPattern sp, Dataset dataset, BindingSet bindings) {
		this(sp, dataset);
		this.bindings = bindings;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public ExternalModel clone() {
		return (ExternalModel)super.clone();
	}

	public Model filter(Model model, BindingSet bindings) {
		Resource subj = (Resource) value(sp.getSubjectVar(), bindings);
		URI pred = (URI) value(sp.getPredicateVar(), bindings);
		Value obj = value(sp.getObjectVar(), bindings);
		Resource[] contexts = contexts(sp, dataset, bindings);
		if (contexts == null)
			return new LinkedHashModel();

		return model.filter(subj, pred, obj, contexts);
	}

	@Override
	public Set<String> getBindingNames() {
		return sp.getBindingNames();
	}

	@Override
	public double cardinality() {
		return model.size();
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(
			BindingSet bindings) throws QueryEvaluationException {
		if (this.bindings != null) {
			QueryBindingSet b = new QueryBindingSet(bindings);
			b.addAll(this.bindings);
			bindings = b;
		}
		CloseableIteration<? extends Statement, QueryEvaluationException> stIter = null;
		try {
			Resource[] contexts = contexts(sp, dataset, bindings);
			if (contexts == null)
				return new EmptyIteration<BindingSet, QueryEvaluationException>();

			Model filtered = filter(model, bindings);
			stIter = new CloseableIteratorIteration<Statement, QueryEvaluationException>(filtered.iterator());

			if (contexts.length == 0 && sp.getScope() == Scope.NAMED_CONTEXTS) {
				stIter = new NamedContextCursor(stIter);
			}
		} catch (ClassCastException e) {
			// Invalid value type for subject, predicate and/or context
			return new EmptyIteration<BindingSet, QueryEvaluationException>();
		}

		// The same variable might have been used multiple times in this
		// StatementPattern, verify value equality in those cases.
		stIter = new StatementPatternCursor(stIter, sp);

		// Return an iterator that converts the statements to var bindings
		return new StatementBindingSetCursor(stIter, sp, bindings);
	}

	private Value value(Var var, BindingSet bindings) {
		if (var == null)
			return null;
		if (var.getValue() != null)
			return var.getValue();
		return bindings.getValue(var.getName());
	}

	private Resource[] contexts(StatementPattern sp, Dataset dataset,
			BindingSet bindings) {
		Value contextValue = value(sp.getContextVar(), bindings);
		if (dataset == null) {
			if (contextValue != null)
				return new Resource[] { (Resource) contextValue };
			return new Resource[0];
		}
		Set<URI> graphs;
		if (sp.getScope() == Scope.DEFAULT_CONTEXTS) {
			graphs = dataset.getDefaultGraphs();
		} else {
			graphs = dataset.getNamedGraphs();
		}

		if (graphs.isEmpty()) {
			// Search zero contexts
			return null;
		}
		if (contextValue == null)
			return graphs.toArray(new Resource[graphs.size()]);
		if (graphs.contains(contextValue)) {
			return new Resource[] { (Resource) contextValue };
		}
		// Statement pattern specifies a context that is not part of the dataset
		return null;
	}

}
