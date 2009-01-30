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
package org.openrdf.repository.object.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectResult;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.traits.PropertyConsumer;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * Converts the repository result into a single Bean.
 * 
 * @author James Leigh
 * 
 */
public class SingleObjectResult extends ObjectIterator<Object, Object>
		implements ObjectResult {

	private final String binding;

	public SingleObjectResult(final ObjectConnection manager,
			final TupleResult result, final String binding,
			final Map<String, URI> predicates) throws StoreException {
		super(new Cursor<Object>() {
			private BindingSet next = result.next();
			private ObjectFactory of = manager.getObjectFactory();

			public Object next() throws StoreException {
				if (next == null)
					return null;
				Map<URI, Set<Value>> properties;
				Value resource = next.getValue(binding);
				properties = readProperties(result, binding, predicates);
				return createRDFObject(of, resource, properties);
			}

			private Map<URI, Set<Value>> readProperties(TupleResult result,
					String binding, Map<String, URI> predicates)
					throws StoreException {
				if (predicates == null) {
					next = result.next();
					return null;
				}
				Value resource = next.getValue(binding);
				Map<URI, Set<Value>> properties = new HashMap<URI, Set<Value>>(
						predicates.size());
				for (URI pred : predicates.values()) {
					properties.put(pred, new HashSet<Value>());
				}
				while (next != null && resource.equals(next.getValue(binding))) {
					for (String name : predicates.keySet()) {
						Value value = next.getValue(name);
						if (value != null) {
							properties.get(predicates.get(name)).add(value);
						}
					}
					next = result.next();
				}
				return properties;
			}

			private Object createRDFObject(ObjectFactory of, Value value,
					Map<URI, Set<Value>> properties) throws StoreException {
				if (value == null)
					return null;
				if (value instanceof Literal)
					return of.createObject((Literal) value);
				if (properties == null)
					return manager.getObject((Resource) value);
				Set<Value> types = properties.get(RDF.TYPE);
				List<URI> list = new ArrayList<URI>(types.size());
				for (Value t : types) {
					if (t instanceof URI) {
						list.add((URI) t);
					}
				}
				RDFObject obj = of.createRDFObject((Resource) value, list);
				if (obj instanceof PropertyConsumer) {
					((PropertyConsumer) obj).usePropertyValues(properties);
				}
				return obj;
			}

			public void close() throws StoreException {
				result.close();
			}
		});
		this.binding = binding;
	}

	public List<String> getBindingNames() throws StoreException {
		return Collections.singletonList(binding);
	}

}