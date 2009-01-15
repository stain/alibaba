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

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.ModelResult;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

public class TypeManager {

	private ContextAwareConnection conn;

	public TypeManager(ContextAwareConnection conn) {
		setConnection(conn);
	}

	public void setConnection(ContextAwareConnection conn) {
		this.conn = conn;
	}

	public ModelResult getTypeStatements(Resource res) throws StoreException {
		return conn.match(res, RDF.TYPE, null);
	}

	public TupleResult evaluateTypeQuery(String qry)
			throws MalformedQueryException, StoreException {
		TupleQuery q;
		q = conn.prepareTupleQuery(SPARQL, qry, null);
		return q.evaluate();
	}

	public void addTypeStatement(Resource resource, URI type)
			throws StoreException {
		if (!RDFS.RESOURCE.equals(type)) {
			conn.add(resource, RDF.TYPE, type);
		}
	}

	public void removeTypeStatement(Resource resource, URI type)
			throws StoreException {
		conn.removeMatch(resource, RDF.TYPE, type);
	}

	public void removeResource(Resource resource) {
		// types are removed with other properties
	}

	public void renameResource(Resource before, Resource after) {
		// types are renamed with other properties
	}
}
