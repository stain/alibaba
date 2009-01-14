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

import javax.xml.namespace.QName;

import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.sesame.roles.SesameEntity;
import org.openrdf.elmo.sesame.roles.SesameManagerAware;
import org.openrdf.model.Resource;

/**
 * Stores the resource and manager for a bean and implements equals, hashCode,
 * and toString.
 * 
 * @author James Leigh
 * 
 */
public class SesameEntitySupport implements Entity, SesameEntity,
		SesameManagerAware {
	private SesameManager manager;
	private Resource resource;

	public SesameEntitySupport(Entity instance) {
		// create a new support instance for every bean created
	}

	public ElmoManager getElmoManager() {
		return manager;
	}

	public SesameManager getSesameManager() {
		return manager;
	}

	public void initSesameManager(SesameManager manager) {
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

}
