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
import org.openrdf.store.StoreException;

public class ObjectCursor implements Cursor<Object> {
	private String binding;
	private Cursor<BindingSet> result;
	private BindingSet next;
	private ObjectFactory of;
	private ObjectConnection manager;

	public ObjectCursor(ObjectConnection manager, Cursor<BindingSet> result,
			String binding) throws StoreException {
		this.binding = binding;
		this.result = result;
		this.next = result.next();
		this.manager = manager;
		this.of = manager.getObjectFactory();
	}

	public Object next() throws StoreException {
		if (next == null)
			return null;
		List<BindingSet> properties;
		Value resource = next.getValue(binding);
		properties = readProperties();
		return createRDFObject(resource, properties);
	}

	private List<BindingSet> readProperties() throws StoreException {
		Value resource = next.getValue(binding);
		List<BindingSet> properties = new ArrayList<BindingSet>();
		while (next != null && resource.equals(next.getValue(binding))) {
			properties.add(next);
			next = result.next();
		}
		return properties;
	}

	private Object createRDFObject(Value value, List<BindingSet> properties)
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
			obj = of.createObject((Resource) value, list);
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
