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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.Query;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.object.result.ObjectArrayResult;
import org.openrdf.repository.object.result.SingleObjectResult;
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

	protected Map<String, URI> properties;

	public ObjectQuery(ObjectConnection manager, TupleQuery query) {
		this(manager, query, null);
	}

	public ObjectQuery(ObjectConnection manager, TupleQuery query, Map<String, URI> properties) {
		assert manager != null;
		assert query != null;
		this.manager = manager;
		this.query = query;
		this.properties = properties;
	}

	public BindingSet getBindings() {
		if (properties == null)
			return query.getBindings();
		MapBindingSet bindings = new MapBindingSet();
		for (Binding binding : query.getBindings()) {
			if (!properties.containsKey(binding.getName())) {
				bindings.addBinding(binding);
			}
		}
		return bindings;
	}

	public void removeBinding(String name) {
		query.removeBinding(name);
	}

	public void setBinding(String name, Value value) {
		query.setBinding(name, value);
	}

	public Dataset getDataset() {
		return query.getDataset();
	}

	public void setDataset(Dataset arg0) {
		query.setDataset(arg0);
	}

	public int getMaxQueryTime() {
		return query.getMaxQueryTime();
	}

	public void setMaxQueryTime(int sec) {
		query.setMaxQueryTime(sec);
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
		} else if (value instanceof RDFObject) {
			setBinding(name, ((RDFObject)value).getResource());
		} else {
			setBinding(name, manager.getObjectFactory().getLiteral(value));
		}
		return this;
	}

	public ObjectQuery setType(String name, Class<?> concept) {
		setBinding(name, manager.getObjectFactory().getType(concept));
		return this;
	}

	public ObjectResult evaluate() throws StoreException {
		TupleResult result = query.evaluate();
		List<String> bindings = result.getBindingNames();
		if (properties != null) {
			bindings = new ArrayList<String>(bindings);
			bindings.removeAll(properties.keySet());
		}
		if (bindings.size() > 1)
			return new ObjectArrayResult(manager, result, bindings);
		return new SingleObjectResult(manager, result, bindings.get(0), properties);
	}

	@Override
	public String toString() {
		return query.toString();
	}
}
