/*
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.openrdf.repository.object;

import java.util.regex.Pattern;

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;

/**
 * This is a work around for http://www.openrdf.org/issues/browse/SES-721
 * 
 * @author James Leigh
 * 
 */
public class InlineSPARQLBaseConnection extends RepositoryConnectionWrapper {

	private static final Pattern BASE = Pattern.compile("\\bBASE\\b",
			Pattern.CASE_INSENSITIVE);

	public InlineSPARQLBaseConnection(Repository repository,
			RepositoryConnection delegate) {
		super(repository, delegate);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query,
			String b) throws MalformedQueryException, RepositoryException {
		return super.prepareBooleanQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareGraphQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareQuery(ql, inlineBase(ql, query, b), b);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String b)
			throws MalformedQueryException, RepositoryException {
		return super.prepareTupleQuery(ql, inlineBase(ql, query, b), b);
	}

	private String inlineBase(QueryLanguage ql, String query, String b) {
		if (ql == QueryLanguage.SPARQL && b != null && b.contains(":")) {
			if (!BASE.matcher(query).find())
				return "BASE <" + b + "> " + query;
		}
		return query;
	}

}
