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

	private QueryLanguage queryLang ;
	private String queryString ;
	private String baseURI ;
	private long lastModified ;
	private long eTagPrefix, eTagSuffix ;
	private String eTag ;
	private ParsedQuery parsedQuery ;
	private TupleExpr query ;
	
	public NamedQueryBase(QueryLanguage ql, String queryString, String baseURI) 
	throws RepositoryException {
		this.eTagPrefix = getNextETagPrefix() ;
		this.eTagSuffix = 0 ;
		this.eTag = getNextETag() ;
		this.queryLang = ql ;
		this.queryString = queryString ;
		this.baseURI = baseURI ;
		this.lastModified = System.currentTimeMillis() ;
        init() ;
	}
		
	public NamedQueryBase
	(long eTagPrefix, long eTagSuffix, String eTag, QueryLanguage ql, String queryString, String baseURI, long lastModified) 
	throws RepositoryException {
		this.eTagPrefix = eTagPrefix ;
		this.eTagSuffix = eTagSuffix ;
		this.eTag = eTag ;
		this.queryLang = ql ;
		this.queryString = queryString ;
		this.baseURI = baseURI ;
		this.lastModified = lastModified ;
		init() ;
    }
		
	private void init() throws RepositoryException {
        try {
			parsedQuery = QueryParserUtil.parseQuery(queryLang, queryString, baseURI);
	        query = parsedQuery.getTupleExpr() ; 		
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e.getMessage()) ;
		} catch (UnsupportedQueryLanguageException e) {
			throw new RepositoryException(e.getMessage()) ;
		}
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
	
	public ParsedQuery getParsedQuery() {
		return parsedQuery;
	}
	
	public TupleExpr getQuery() {
		return query;
	}

	public synchronized void update(long time) {
		lastModified = time ;
		eTag = getNextETag() ;
	}

	protected long getResultETagPrefix() {
		return eTagPrefix ;
	}
	
	protected long getResultETagSuffix() {
		return eTagSuffix ;
	}
	
	private static synchronized long getNextETagPrefix() {
		return eTagPrefixCounter = Math.max(System.currentTimeMillis(), eTagPrefixCounter + 1);
	}

	private String getNextETag() {
		return Long.toString(eTagPrefix, 32) + Long.toString(eTagSuffix++, 32) ;
	}

}
