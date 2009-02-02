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
import java.util.List;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.traits.PropertyConsumer;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author James Leigh
 * 
 */
public class ObjectArrayCursor implements Cursor<Object[]> {

	private List<String> bindings;
	private Cursor<BindingSet> result;
	private BindingSet next;
	private ObjectFactory of;
	private ObjectConnection manager;

	public ObjectArrayCursor(ObjectConnection manager, TupleResult result,
			List<String> bindings) throws StoreException {
		this.bindings = bindings;
		this.result = result;
		this.next = result.next();
		this.manager = manager;
		this.of = manager.getObjectFactory();
	}

	public Object[] next() throws StoreException {
		if (next == null)
			return null;
		List<BindingSet> properties;
		Value[] resources = new Value[bindings.size()];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = next.getValue(bindings.get(i));
		}
		properties = readProperties(resources);
		Object[] result = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			result[i] = createRDFObject(resources[i], bindings.get(i), properties);
		}
		return result;
	}

	private List<BindingSet> readProperties(Value... values)
			throws StoreException {
		List<BindingSet> properties = new ArrayList<BindingSet>();
		while (next != null) {
			for (int i = 0; i < values.length; i++) {
				if (!values[i].equals(next.getValue(bindings.get(i))))
					return properties;
			}
			properties.add(next);
			next = result.next();
		}
		return properties;
	}

	private Object createRDFObject(Value value, String binding, List<BindingSet> properties)
			throws StoreException {
		if (value == null)
			return null;
		if (value instanceof Literal)
			return of.createObject((Literal) value);
		Object obj;
		if (properties.get(0).hasBinding(binding + "_class")) {
			List<URI> list = new ArrayList<URI>(properties.size());
			for (BindingSet bindings : properties) {
				Value t = bindings.getValue(binding + "_class");
				if (t instanceof URI) {
					list.add((URI) t);
				}
			}
			obj = of.createRDFObject((Resource) value, list);
		} else {
			obj = manager.getObject(value);
		}
		if (obj instanceof PropertyConsumer) {
			((PropertyConsumer) obj).usePropertyBindings(binding, properties);
		}
		return obj;
	}

	public void close() throws StoreException {
		result.close();
	}
}
