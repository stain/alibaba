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

import javax.xml.namespace.QName;

import org.openrdf.elmo.sesame.ElmoSingleQueryResult;
import org.openrdf.elmo.sesame.ElmoTupleQueryResult;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.Query;
import org.openrdf.query.TupleQuery;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * Implements {@link ObjectQuery} for use with SesameManager.
 * 
 * @author James Leigh
 */
public class ObjectQuery implements Query {

	protected ObjectConnection manager;

	protected TupleQuery query;

	private int firstResult;

	private int maxResults;

	public ObjectQuery(ObjectConnection manager, TupleQuery query) {
		this.manager = manager;
		this.query = query;
	}

	public BindingSet getBindings() {
		return query.getBindings();
	}

	public Dataset getDataset() {
		return query.getDataset();
	}

	public int getMaxQueryTime() {
		return query.getMaxQueryTime();
	}

	public void removeBinding(String arg0) {
		query.removeBinding(arg0);
	}

	public void setDataset(Dataset arg0) {
		query.setDataset(arg0);
	}

	public void setMaxQueryTime(int arg0) {
		query.setMaxQueryTime(arg0);
	}

	@SuppressWarnings("unchecked")
	private ObjectResult evaluateQuery() throws StoreException {
		TupleResult result = query.evaluate();
		int max = maxResults <= 0 ? 0 : maxResults + firstResult;
		if (result.getBindingNames().size() > 1)
			return new ElmoTupleQueryResult(manager, result, max);
		return new ElmoSingleQueryResult(manager, result, max);
	}

	public ObjectResult evaluate() throws StoreException {
		ObjectResult result = evaluateQuery();
		if (firstResult > 0) {
			for (int i = 0; i < firstResult && result.hasNext(); i++) {
				result.next();
			}
		}
		return result;
	}

	public ObjectQuery setFirstResult(int startPosition) {
		this.firstResult = startPosition;
		return this;
	}

	public ObjectQuery setMaxResults(int maxResult) {
		this.maxResults = maxResult;
		return this;
	}

	public boolean getIncludeInferred() {
		return query.getIncludeInferred();
	}

	public void setIncludeInferred(boolean include) {
		query.setIncludeInferred(include);
	}

	public ObjectQuery setParameter(String name, Object value) {
		if (value == null) {
			setBinding(name, null);
		} else {
			setBinding(name, manager.getValue(value));
		}
		return this;
	}

	public ObjectQuery setType(String name, Class<?> concept) {
		setBinding(name, manager.getRoleMapper().findType(concept));
		return this;
	}

	public ObjectQuery setQName(String name, QName qname) {
		setBinding(name, manager.getResourceManager().createResource(qname));
		return this;
	}

	@Override
	public String toString() {
		if (query == null)
			return super.toString();
		return query.toString();
	}

	public void setBinding(String name, Value value) {
		if (query == null)
			throw new UnsupportedOperationException();
		query.setBinding(name, value);
	}
}
