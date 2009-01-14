/*
 * Copyright (c) 2007, James Leigh All rights reserved.
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
package org.openrdf.elmo.sesame;

import java.util.Set;

import javax.xml.namespace.QName;

import org.openrdf.elmo.sesame.helpers.PropertyChanger;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.elmo.sesame.roles.SesameManagerAware;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ElmoIOException;
import org.openrdf.store.StoreException;

/**
 * Stores the resource and manager for a bean and implements equals, hashCode,
 * and toString.
 * 
 * @author James Leigh
 * 
 */
public class SesameEntitySupport implements RDFObject, SesameEntity,
		SesameManagerAware {
	private ObjectConnection manager;
	private Resource resource;

	public SesameEntitySupport(RDFObject instance) {
		// create a new support instance for every bean created
	}

	public ObjectConnection getElmoManager() {
		return manager;
	}

	public ObjectConnection getSesameManager() {
		return manager;
	}

	public void initSesameManager(ObjectConnection manager) {
		this.manager = manager;
	}

	public Resource getSesameResource() {
		return resource;
	}

	public void initSesameResource(Resource resource) {
		this.resource = resource;
	}

	public QName getQName() {
		return manager.getResourceManager().createQName(resource);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SesameEntity
				&& resource.equals(((SesameEntity) obj).getSesameResource());
	}

	@Override
	public int hashCode() {
		return resource.hashCode();
	}

	@Override
	public String toString() {
		return resource.toString();
	}

	public void refresh() {
		// do nothing
	}

	public Set<Object> get(String pred) {
		return getProperty(pred);
	}

	public void set(String pred, Set<?> values) {
		getProperty(pred).setAll(values);
	}

	private SesameProperty getProperty(String pred) {
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
		URI uri = getPredicate(pred, prefix, local);
		return new SesameProperty(this, new PropertyChanger(uri));
	}

	private URI getPredicate(String pred, String prefix, String local) {
		ContextAwareConnection con = manager.getConnection();
		ValueFactory vf = con.getValueFactory();
		if (pred.contains("/"))
			return vf.createURI(pred);
		try {
			String ns = con.getNamespace(prefix);
			if (ns == null)
				return vf.createURI(pred);
			return vf.createURI(ns, local);
		} catch (StoreException e) {
			throw new ElmoIOException(e);
		}
	}

}
