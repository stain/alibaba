/*
 * Copyright (c) 2008-2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;

public class TypeManager {

	private ObjectConnection conn;

	public void setConnection(ObjectConnection conn) {
		this.conn = conn;
	}

	public Collection<URI> getTypes(Resource res) throws RepositoryException {
		RepositoryResult<Statement> match = conn.getStatements(res, RDF.TYPE, null);
		try {
			if (!match.hasNext())
				return Collections.emptySet();
			Value obj = match.next().getObject();
			if (obj instanceof URI && !match.hasNext())
				return Collections.singleton((URI) obj);
			List<URI> types = new ArrayList<URI>();
			if (obj instanceof URI) {
				types.add((URI) obj);
			}
			while (match.hasNext()) {
				obj = match.next().getObject();
				if (obj instanceof URI) {
					types.add((URI) obj);
				}
			}
			return types;
		} finally {
			if (match != null)
				match.close();
		}
	}

	public void addTypeStatement(Resource resource, URI type)
			throws RepositoryException {
		if (!RDFS.RESOURCE.equals(type)) {
			conn.add(resource, RDF.TYPE, type);
		}
	}

	public void removeTypeStatement(Resource resource, URI type)
			throws RepositoryException {
		conn.remove(resource, RDF.TYPE, type);
	}
}
