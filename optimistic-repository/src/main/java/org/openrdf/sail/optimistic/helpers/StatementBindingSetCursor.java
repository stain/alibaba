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

import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;

/**
 * Converts a statement cursor into a binding cursor.
 * 
 * @author James Leigh
 *
 */
public class StatementBindingSetCursor extends
		ConvertingIteration<Statement, BindingSet, QueryEvaluationException> {

	private Var subjVar;
	private Var predVar;
	private Var objVar;
	private Var conVar;
	private BindingSet bindings;

	public StatementBindingSetCursor(
			Iteration<? extends Statement, ? extends QueryEvaluationException> iter,
			StatementPattern sp, BindingSet bindings) {
		super(iter);
		subjVar = sp.getSubjectVar();
		predVar = sp.getPredicateVar();
		objVar = sp.getObjectVar();
		conVar = sp.getContextVar();
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(Statement st) {
		QueryBindingSet result = new QueryBindingSet(bindings);

		if (subjVar != null && !result.hasBinding(subjVar.getName())) {
			result.addBinding(subjVar.getName(), st.getSubject());
		}
		if (predVar != null && !result.hasBinding(predVar.getName())) {
			result.addBinding(predVar.getName(), st.getPredicate());
		}
		if (objVar != null && !result.hasBinding(objVar.getName())) {
			result.addBinding(objVar.getName(), st.getObject());
		}
		if (conVar != null && !result.hasBinding(conVar.getName())
				&& st.getContext() != null) {
			result.addBinding(conVar.getName(), st.getContext());
		}

		return result;
	}
}
