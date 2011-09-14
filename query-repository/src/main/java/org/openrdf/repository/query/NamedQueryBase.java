/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
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

package org.openrdf.repository.query;

import org.openrdf.model.URI;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.query.NamedQueryRepository.NamedQuery;

/**
 * Base implementation for named queries
 * Defines accessors and revises result last modified and eTag on update()
 * 
 * @author Steve Battle
 *
 */

public class NamedQueryBase implements NamedQuery {
	
	private static long eTagPrefixCounter = 0 ;

	protected URI uri ;
	protected QueryLanguage queryLang ;
	protected String queryString ;
	protected String baseURI ;
	protected long lastModified ;
	protected long eTagPrefix, eTagSuffix ;
	protected String eTag = getNextETag() ;
	protected ParsedQuery parsedQuery ;
	protected TupleExpr query ;
	
	public NamedQueryBase(URI uri, QueryLanguage ql, String queryString, String baseURI) 
	throws MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		this(getNextETagPrefix(), 0, uri, ql, queryString, baseURI, System.currentTimeMillis()) ;
	}
	
	protected NamedQueryBase(long eTagPrefix, long eTagSuffix, URI uri, QueryLanguage ql, String queryString, String baseURI, long lastModified) 
	throws MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException {
		super();
		this.eTagPrefix = eTagPrefix ;
		this.eTagSuffix = eTagSuffix ;
		this.uri = uri ;
		this.queryLang = ql ;
		this.queryString = queryString ;
		this.baseURI = baseURI ;
		this.lastModified = lastModified ;
		
        parsedQuery = QueryParserUtil.parseQuery(ql, queryString, baseURI);
        query = parsedQuery.getTupleExpr() ;       
	}

	public URI getUri() {
		return uri;
	}

	private static synchronized long getNextETagPrefix() {
		return eTagPrefixCounter = Math.max(System.currentTimeMillis(), eTagPrefixCounter + 1);
	}

	public QueryLanguage getQueryLanguage() {
		return queryLang ;
	}
	
	public String getQueryString() {
		return queryString ;
	}
	
	public String getBaseURI() {
		return baseURI ;
	}

	public long getResultLastModified() {
		return lastModified ;
	}
	
	public String getResultETag() {
		return eTag ;
	}
	
	private String getNextETag() {
		return Long.toString(eTagPrefix, 32) + Long.toString(eTagSuffix++, 32) ;
	}
	
	public ParsedQuery getParsedQuery() {
		return parsedQuery;
	}

	protected void update(long time) {
		lastModified = time ;
		eTag = getNextETag() ;
	}

}
