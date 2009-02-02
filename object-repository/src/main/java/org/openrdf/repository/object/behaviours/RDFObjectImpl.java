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
package org.openrdf.repository.object.behaviours;

import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.composition.helpers.ObjectQueryFactory;
import org.openrdf.repository.object.composition.helpers.PropertySet;
import org.openrdf.repository.object.composition.helpers.PropertySetModifier;
import org.openrdf.repository.object.composition.helpers.RemotePropertySet;
import org.openrdf.repository.object.exceptions.ObjectStoreException;
import org.openrdf.repository.object.traits.InternalRDFObject;
import org.openrdf.store.StoreException;

/**
 * Stores the resource and manager for a bean and implements equals, hashCode,
 * and toString.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectImpl implements InternalRDFObject, RDFObject {
	private ObjectConnection manager;
	private ObjectQueryFactory factory;
	private Resource resource;

	public void initRDFObject(Resource resource, ObjectQueryFactory factory, ObjectConnection manager) {
		this.manager = manager;
		this.factory = factory;
		this.resource = resource;
	}

	public ObjectConnection getObjectConnection() {
		return manager;
	}

	public ObjectQueryFactory getObjectQueryFactory() {
		return factory;
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof RDFObject
				&& resource.equals(((RDFObject) obj).getResource());
	}

	@Override
	public int hashCode() {
		return resource.hashCode();
	}

	@Override
	public String toString() {
		return resource.toString();
	}

	public Set<Object> get(String pred) {
		return getProperty(pred).getAll();
	}

	public void set(String pred, Set<?> values) {
		getProperty(pred).setAll(values);
	}

	public Object getSingle(String pred) {
		return getProperty(pred).getSingle();
	}

	public void setSingle(String pred, Object value) {
		getProperty(pred).setSingle(value);
	}

	private PropertySet getProperty(String pred) {
		String prefix;
		String local;
		int idx = pred.indexOf(':');
		if (idx > 0) {
			prefix = pred.substring(0, idx);
			local = pred.substring(idx + 1);
		} else {
			prefix = "";
			local = pred;
		}
		URI uri = getPredicate(prefix, local);
		return new RemotePropertySet(this, new PropertySetModifier(uri));
	}

	private URI getPredicate(String prefix, String local) {
		ContextAwareConnection con = manager;
		ValueFactory vf = con.getValueFactory();
		try {
			String ns = con.getNamespace(prefix);
			if (ns == null)
				throw new IllegalArgumentException("Unknown prefix: " + prefix);
			return vf.createURI(ns, local);
		} catch (StoreException e) {
			throw new ObjectStoreException(e);
		}
	}

}
