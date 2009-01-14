/*
 * Copyright (c) 2007-2009, James Leigh All rights reserved.
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

import org.openrdf.elmo.ElmoEntityResolver;
import org.openrdf.elmo.LiteralManager;
import org.openrdf.elmo.RoleMapper;
import org.openrdf.elmo.impl.ElmoEntityResolverImpl;
import org.openrdf.elmo.sesame.SesameResourceManager;
import org.openrdf.elmo.sesame.SesameTypeManager;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.store.StoreException;

/**
 * Creates SesameBeanManagers.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private ElmoEntityResolverImpl<URI> resolver;

	private LiteralManager<URI, Literal> literalManager;

	private RoleMapper<URI> mapper;

	public RoleMapper<URI> getRoleMapper() {
		return mapper;
	}

	public void setRoleMapper(RoleMapper<URI> mapper) {
		this.mapper = mapper;
	}

	public LiteralManager<URI, Literal> getLiteralManager() {
		return literalManager;
	}

	public void setLiteralManager(LiteralManager<URI, Literal> literalManager) {
		this.literalManager = literalManager;
	}

	public ElmoEntityResolver<URI> getElmoEntityResolver() {
		return resolver;
	}

	public void setElmoEntityResolver(ElmoEntityResolverImpl<URI> resolver) {
		this.resolver = resolver;
	}

	@Override
	public ObjectConnection getConnection() throws StoreException {
		RepositoryConnection conn = getDelegate().getConnection();
		ObjectConnection con = new ObjectConnection(this, conn);
		con.setIncludeInferred(isIncludeInferred());
		con.setMaxQueryTime(getMaxQueryTime());
		con.setQueryLanguage(getQueryLanguage());
		con.setReadContexts(getReadContexts());
		con.setAddContexts(getAddContexts());
		con.setRemoveContexts(getRemoveContexts());
		con.setArchiveContexts(getArchiveContexts());
		con.setLiteralManager(literalManager);
		con.setRoleMapper(mapper);
		SesameResourceManager rolesManager;
		rolesManager = new SesameResourceManager();
		rolesManager.setConnection(con);
		rolesManager.setSesameTypeRepository(new SesameTypeManager(con));
		rolesManager.setRoleMapper(mapper);
		rolesManager.setElmoEntityResolver(resolver);
		con.setResourceManager(rolesManager);
		return con;
	}

}
