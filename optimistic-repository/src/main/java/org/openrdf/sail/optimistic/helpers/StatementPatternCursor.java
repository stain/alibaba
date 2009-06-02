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

import info.aduna.iteration.FilterIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;

/**
 * When variables occur twice within a statement pattern, this class ensures the
 * returned values also match.
 * 
 * @author James Leigh
 * 
 */
public class StatementPatternCursor extends FilterIteration<Statement, QueryEvaluationException> {

	private Var subjVar;
	private Var predVar;
	private Var objVar;
	private Var conVar;

	public StatementPatternCursor(
			Iteration<? extends Statement, ? extends QueryEvaluationException> iter, StatementPattern sp) {
		super(iter);
		subjVar = sp.getSubjectVar();
		predVar = sp.getPredicateVar();
		objVar = sp.getObjectVar();
		conVar = sp.getContextVar();
	}

	@Override
	protected boolean accept(Statement st) {
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource context = st.getContext();

		if (subjVar != null) {
			if (subjVar.equals(predVar) && !subj.equals(pred)) {
				return false;
			}
			if (subjVar.equals(objVar) && !subj.equals(obj)) {
				return false;
			}
			if (subjVar.equals(conVar) && !subj.equals(context)) {
				return false;
			}
		}

		if (predVar != null) {
			if (predVar.equals(objVar) && !pred.equals(obj)) {
				return false;
			}
			if (predVar.equals(conVar) && !pred.equals(context)) {
				return false;
			}
		}

		if (objVar != null) {
			if (objVar.equals(conVar) && !obj.equals(context)) {
				return false;
			}
		}

		return true;
	}
}
