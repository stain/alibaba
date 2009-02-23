/*
 * Copyright (c) 2008-2009, Zepheira All rights reserved.
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
package org.openrdf.repository.object.codegen.model;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

public class RDFEntity {

	protected Model model;

	protected Resource self;

	public RDFEntity(Model model, Resource self) {
		this.model = model;
		this.self = self;
	}

	public Model getModel() {
		return model;
	}

	public URI getURI() {
		if (self instanceof URI)
			return (URI) self;
		return null;
	}

	public boolean isA(URI type) {
		return model.contains(self, RDF.TYPE, type);
	}

	public Set<?> get(URI pred) {
		Set<Object> set = new HashSet<Object>();
		for (Value value : model.filter(self, pred, null).objects()) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				if (model.contains(subj, RDF.TYPE, OWL.CLASS)) {
					set.add(new RDFClass(model, subj));
				} else {
					set.add(value.stringValue());
				}
			} else {
				set.add(value.stringValue());
			}
		}
		return set;
	}

	public Resource getResource(URI pred) {
		return model.filter(self, pred, null).objectResource();
	}

	public Set<? extends Value> getValues(URI pred) {
		return model.filter(self, pred, null).objects();
	}

	public Set<String> getStrings(URI pred) {
		Set<String> set = new HashSet<String>();
		for (Value value : model.filter(self, pred, null).objects()) {
			set.add(value.stringValue());
		}
		return set;
	}

	public String getString(URI pred) {
		return model.filter(self, pred, null).objectString();
	}

	public RDFClass getRDFClass(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFClass(model, subj);
	}

	public Set<RDFClass> getRDFClasses(URI pred) {
		Set<RDFClass> set = new HashSet<RDFClass>();
		for (Value value : model.filter(self, pred, null).objects()) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				set.add(new RDFClass(model, subj));
			}
		}
		return set;
	}

}
