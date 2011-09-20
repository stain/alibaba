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

import java.io.File;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.repository.RepositoryException;

/**
 * Convenience methods for wrapping a persistent named query
 * 
 * @author Steve Battle
 *
 */

public abstract class PersistentNamedQueryWrapper implements PersistentNamedQuery {
	
	private PersistentNamedQuery namedQuery ;
	
	public QueryLanguage getQueryLanguage() {
		return namedQuery.getQueryLanguage() ;
	}

	public String getQueryString() {
		return namedQuery.getQueryString() ;
	}

	public String getBaseURI() {
		return namedQuery.getBaseURI() ;
	}

	public long getResultLastModified() {
		return namedQuery.getResultLastModified() ;
	}

	public String getResponseTag() {
		return namedQuery.getResponseTag() ;
	}

	public TupleExpr getQuery() {
		return namedQuery.getQuery();
	}

	public ParsedQuery getParsedQuery() {
		return namedQuery.getParsedQuery() ;
	}		

	public void update(long time) {
		namedQuery.update(time);
	}
	
	/* methods supporting persistence */
	
	public void cease(File dataDir, URI uri) {
		namedQuery.cease(dataDir, uri);
	}
	
	public void desist(File dataDir, URI uri) throws RepositoryException {
		namedQuery.desist(dataDir, uri);
	}

	public static Map<URI, PersistentNamedQuery> persist(File dataDir, ValueFactory vf) 
	throws RepositoryException {
		return PersistentNamedQueryImpl.persist(dataDir, vf) ;
	}
	
	/* Constructors */

	protected PersistentNamedQueryWrapper() {
	}

	protected PersistentNamedQueryWrapper(PersistentNamedQuery namedQuery) {
		this.namedQuery = namedQuery ;
	}

	protected void setNamedQuery(PersistentNamedQuery namedQuery) {
		this.namedQuery = namedQuery;
	}


}
