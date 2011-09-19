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
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryWrapper;


/**
 * Delegating repository for named queries
 * 
 * @author Steve Battle
 *
 */

public class DelegatingNamedQueryRepository extends RepositoryWrapper implements NamedQueryRepository {
	// a local reference to the, possibly nested, named query repository delegate
	private NamedQueryRepository delegate ;
	
	/* constructors */

	public DelegatingNamedQueryRepository() {
		super();
	}
	
	/* Use the constructor below to set a named query primary delegate
	 * Use setDelegate to set an a non-named-query delegate if necessary
	 */

	public DelegatingNamedQueryRepository(Repository delegate) {
		super(delegate);
		if (delegate instanceof NamedQueryRepository) {
			this.delegate = (NamedQueryRepository) delegate ;
		}
	}
	
	/** Use this constructor to set a primary delegate and a nested named query delegate */
	
	public DelegatingNamedQueryRepository
		(Repository immediateDelegate, NamedQueryRepository nestedDelegate) {
		super(immediateDelegate);
		this.delegate = nestedDelegate ;
	}
	
	/** Set only the primary delegate 
	 * if the named query repository is nested it remains unset, use setNamedQueryDelegate() subsequently */

	@Override
	public void setDelegate(Repository delegate) {
		super.setDelegate(delegate);
		if (delegate instanceof NamedQueryRepository) {
			this.delegate = (NamedQueryRepository) delegate ;
		}
	}
	
	/* Set only the nested named query delegate, not the immediate delegate 
	 * Do NOT use this method to set the immediate delegate - even if it is a named query delegate
	 * The nestedDelegate MUST be in the delegate chain of the primary delegate
	 **/
	
	public void setNamedQueryDelegate(NamedQueryRepository nestedDelegate) {
		this.delegate = nestedDelegate ;
	}
	
	/* Delegate support for the NamedQueryRepository interface */

	public NamedQuery createNamedQuery(URI uri, QueryLanguage ql, String queryString, String baseURI) 
	throws RepositoryException {
		return delegate.createNamedQuery(uri, ql, queryString, baseURI) ;
	}

	public void removeNamedQuery(URI uri) throws RepositoryException {
		delegate.removeNamedQuery(uri) ;
	}

	public URI[] getNamedQueryURIs() throws RepositoryException {
		return delegate.getNamedQueryURIs() ;
	}

	public NamedQuery getNamedQuery(URI uri) throws RepositoryException {
		return delegate.getNamedQuery(uri) ;
	}

}
