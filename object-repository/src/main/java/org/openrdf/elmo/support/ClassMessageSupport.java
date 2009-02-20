/*
 * Copyright (c) 2008, Zepheira All rights reserved.
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
package org.openrdf.elmo.codegen.support;

import static org.openrdf.elmo.codegen.vocabulary.ELMO.LITERAL_RESPONSE;
import static org.openrdf.elmo.codegen.vocabulary.ELMO.OBJECT_RESPONSE;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.concepts.owl.OwlProperty;
import org.openrdf.concepts.owl.Restriction;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.ElmoQuery;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.vocabulary.ELMO;

public abstract class ClassMessageSupport implements CodeMessageClass {
	private static final String OWL = "http://www.w3.org/2002/07/owl#";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";

	private static final String PREFIX = "PREFIX owl:<" + OWL
			+ ">\nPREFIX rdfs:<" + RDFS + ">\nPREFIX msg:<" + ELMO.NAMESPACE
			+ ">\n";
	private static final String MSG_TARGET_THIS = PREFIX + "SELECT ?msg "
			+ "WHERE { ?msg rdfs:subClassOf ?res . "
			+ "?res owl:onProperty msg:target . "
			+ "?res owl:allValuesFrom ?this }";
	private static final String WHERE_PROP_DOMAIN_TYPE = PREFIX
			+ "SELECT DISTINCT ?prop WHERE { { ?prop rdfs:domain ?type } "
			+ "UNION { ?prop rdfs:domain ?sup . ?type rdfs:subClassOf ?sup . "
			+ "?type rdfs:isDefinedBy ?ont . ?sup rdfs:isDefinedBy ?ont } "
			+ "OPTIONAL { ?prop a ?ann FILTER (?ann = owl:AnnotationProperty) } "
			+ "FILTER (!bound(?ann)) } " + "ORDER BY ?prop";

	public Iterable<CodeMessageClass> getMessageTypes() {
		ElmoQuery query = getElmoManager().createQuery(MSG_TARGET_THIS);
		query.setParameter("this", this);
		return query.getResultList();
	}

	public boolean isMessageClass() {
		return isMessage(this, new HashSet<Class>());
	}

	public List<Property> getParameters() {
		ElmoQuery query = getElmoManager().createQuery(WHERE_PROP_DOMAIN_TYPE);
		query.setParameter("type", this);
		return query.getResultList();
	}

	public Property getResponseProperty() {
		ElmoManager manager = getElmoManager();
		Property obj = (Property) manager.find(OBJECT_RESPONSE);
		Property lit = (Property) manager.find(LITERAL_RESPONSE);
		boolean objUsed = false;
		boolean litUsed = false;
		boolean obj0 = false;
		boolean lit0 = false;
		for (Class c : getRdfsSubClassOf()) {
			if (c instanceof Restriction) {
				Restriction r = (Restriction) c;
				OwlProperty property = r.getOwlOnProperty();
				BigInteger card = r.getOwlCardinality();
				BigInteger max = r.getOwlMaxCardinality();
				if (obj.equals(property)) {
					objUsed = true;
					if (card != null && 0 == card.intValue()) {
						obj0 = true;
					} else if (max != null && 0 == max.intValue()) {
						obj0 = true;
					}
				} else if (lit.equals(property)) {
					litUsed = true;
					if (card != null && 0 == card.intValue()) {
						lit0 = true;
					} else if (max != null &&0 == max.intValue()) {
						lit0 = true;
					}
				}
			}
		}
		if (obj0 && !lit0)
			return lit;
		if (litUsed && !objUsed)
			return lit;
		return obj;
	}

	private boolean isMessage(Class message,
			Set<Class> set) {
		if (ELMO.MESSAGE.equals(message.getQName()))
			return true;
		set.add(message);
		for (Class sup : message.getRdfsSubClassOf()) {
			if (!set.contains(sup) && isMessage(sup, set))
				return true;
		}
		return false;
	}
}
