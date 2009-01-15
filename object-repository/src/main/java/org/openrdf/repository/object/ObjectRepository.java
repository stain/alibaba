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

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareRepository;
import org.openrdf.repository.object.composition.ClassResolver;
import org.openrdf.repository.object.managers.LiteralManager;
import org.openrdf.repository.object.managers.ResourceManager;
import org.openrdf.repository.object.managers.RoleMapper;
import org.openrdf.repository.object.managers.TypeManager;
import org.openrdf.store.StoreException;

/**
 * Creates SesameBeanManagers.
 * 
 * @author James Leigh
 * 
 */
public class ObjectRepository extends ContextAwareRepository {
	private ClassResolver resolver;

	private LiteralManager literalManager;

	private RoleMapper mapper;

	public RoleMapper getRoleMapper() {
		return mapper;
	}

	public void setRoleMapper(RoleMapper mapper) {
		this.mapper = mapper;
	}

	public LiteralManager getLiteralManager() {
		return literalManager;
	}

	public void setLiteralManager(LiteralManager literalManager) {
		this.literalManager = literalManager;
	}

	public ClassResolver getClassResolver() {
		return resolver;
	}

	public void setClassResolver(ClassResolver resolver) {
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
		ResourceManager rolesManager;
		rolesManager = new ResourceManager();
		rolesManager.setConnection(con);
		rolesManager.setSesameTypeRepository(new TypeManager(con));
		rolesManager.setRoleMapper(mapper);
		rolesManager.setClassResolver(resolver);
		con.setResourceManager(rolesManager);
		return con;
	}

}
