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

package org.openrdf.repository.query.config;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.query.DelegatingNamedQueryRepository;
import org.openrdf.repository.query.NamedQueryRepository;
import org.openrdf.repository.query.NamedQueryRepositoryWrapper;


/**
 * Factory for Named Query Repositories
 * 
 * @author Steve Battle
 *
 */

public class NamedQueryRepositoryFactory implements RepositoryFactory {
	
	public static final String REPOSITORY_TYPE = "openrdf:NamedQueryRepository";

	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	public RepositoryImplConfig getConfig() {
		return new NamedQueryRepositoryConfig() ;
	}	

	/**
	 * Create a Repository from a previously initialised delegate.
	 */
	
	public NamedQueryRepository createRepository(Repository delegate)
	throws RepositoryConfigException, RepositoryException {
		return createRepository(getConfig(), delegate) ;
	}
	
	/**
	 * Create a Repository from a previously initialised delegate (with config).
	 */
	
	public NamedQueryRepository createRepository(RepositoryImplConfig config, Repository delegate) 
	throws RepositoryConfigException, RepositoryException {
		
		// create a repository without using a delegate
		if (delegate==null) return getRepository(config) ;
		
		if (delegate instanceof NamedQueryRepository) {
			// no need to further wrap or delegate
			return (NamedQueryRepository) delegate ;
		}
		
		// look for a nested named query repository in the delegate chain
		NamedQueryRepository nestedDelegate = getNamedQueryDelegate(delegate) ;
		
		if (nestedDelegate==null) {
			// There is no named query delegate, need to wrap it
			
			// The wrapper expects a notifying repository
			NotifyingRepositoryWrapper notifier ;
			if (delegate instanceof NotifyingRepository) {
				notifier = (NotifyingRepositoryWrapper) delegate ;
			} else notifier = new NotifyingRepositoryWrapper(delegate) ;
			
			return new NamedQueryRepositoryWrapper(notifier) ;
		}
		
		// Delegate to the nested named query repository
		return new DelegatingNamedQueryRepository(delegate, nestedDelegate) ;
	}
		
	/**
	 * Create an uninitialised Repository without a delegate.
	 * NO DATA-DIR CONFIGURED FOR PERSISTENCE
	 */

	public NamedQueryRepository getRepository(RepositoryImplConfig configuration) 
	throws RepositoryConfigException {
		if (configuration instanceof NamedQueryRepositoryConfig) {
			
			return new NamedQueryRepositoryWrapper() ;
			
		}
		throw new RepositoryConfigException("Invalid configuration class: " + configuration.getClass());
	}

	/* search the delegate chain for a named query repository */
	
	private NamedQueryRepository getNamedQueryDelegate(Repository delegate) {
		while (delegate!=null) {
			if (delegate instanceof NamedQueryRepository) {
				return (NamedQueryRepository) delegate ;
			}
			else if (delegate instanceof RepositoryWrapper) {
				delegate = ((RepositoryWrapper) delegate).getDelegate() ;
			}
			else break ;
		}
		return null ;
	}

}
