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
package org.openrdf.repository.object.compiler.model;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.RDFList;
import org.openrdf.repository.object.compiler.source.JavaClassBuilder;
import org.openrdf.repository.object.compiler.source.JavaBuilder;
import org.openrdf.repository.object.vocabulary.OBJ;

public class RDFClass extends RDFEntity {

	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");

	public RDFClass(Model model, Resource self) {
		super(model, self);
	}

	public BigInteger getBigInteger(URI pred) {
		Value value = model.filter(self, pred, null).objectValue();
		if (value == null)
			return null;
		return new BigInteger(value.stringValue());
	}

	public RDFProperty getRDFProperty(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	public List<? extends Value> getList(URI pred) {
		List<? extends Value> list = null;
		for (Value obj : model.filter(self, pred, null).objects()) {
			if (list == null && obj instanceof Resource) {
				list = new RDFList(model, (Resource) obj).asList();
			} else {
				List<? extends Value> other = new RDFList(model, (Resource) obj)
						.asList();
				if (!list.equals(other)) {
					other.removeAll(list);
					((List) list).addAll(other);
				}
			}
		}
		return list;
	}

	public List<? extends RDFClass> getClassList(URI pred) {
		List<? extends Value> list = getList(pred);
		if (list == null)
			return null;
		List<RDFClass> result = new ArrayList<RDFClass>();
		for (Value value : list) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				result.add(new RDFClass(model, subj));
			}
		}
		return result;
	}

	private Collection<RDFProperty> getDeclaredProperties() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (prop instanceof URI) {
				set.add(prop.stringValue());
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>(set.size());
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFClass getRange(RDFProperty property) {
		if (property.isLocalized()) {
			return new RDFClass(property.getModel(), XMLSchema.STRING);
		}
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					RDFClass type = c.getRDFClass(OWL.ALLVALUESFROM);
					if (type != null) {
						return type;
					}
				}
			}
		}
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION) || c.equals(this))
				continue;
			RDFClass type = ((RDFClass) c).getRange(property);
			if (type != null) {
				return type;
			}
		}
		for (RDFClass r : property.getRDFClasses(RDFS.RANGE)) {
			return r;
		}
		for (RDFProperty p : property.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			RDFClass superRange = getRange(p);
			if (superRange != null) {
				return superRange;
			}
		}
		return null;
	}

	public boolean isFunctional(RDFProperty property) {
		if (property.isA(OWL.FUNCTIONALPROPERTY))
			return true;
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						return true;
					}
				}
			}
		}
		if (property.getStrings(OBJ.LOCALIZED).contains("true"))
			return true;
		RDFClass range = getRange(property);
		if (range == null)
			return false;
		return NOTHING.equals(range.getURI());
	}

	public boolean isEmpty() {
		return getDeclaredProperties().isEmpty() && getMessageTypes().isEmpty();
	}

	public File generateSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFile(dir, resolver);
		JavaClassBuilder jcb = new JavaClassBuilder(source);
		JavaBuilder builder = new JavaBuilder(jcb, resolver);
		if (isDatatype()) {
			builder.classHeader(this);
			builder.stringConstructor(this);
		} else {
			builder.interfaceHeader(this);
			builder.constants(this);
			for (RDFProperty prop : getDeclaredProperties()) {
				if (prop.isMethodOrTrigger())
					continue;
				builder.property(this, prop);
			}
			for (RDFClass type : getMessageTypes()) {
				builder.message(type);
			}
		}
		builder.close();
		return source;
	}

	public boolean isDatatype() {
		return isA(RDFS.DATATYPE) || self.equals(RDFS.LITERAL);
	}

	public Collection<RDFClass> getMessageTypes() {
		List<RDFClass> list = new ArrayList<RDFClass>();
		for (Resource res : model.filter(null, OWL.ALLVALUESFROM, self)
				.subjects()) {
			if (model.contains(res, OWL.ONPROPERTY, OBJ.TARGET)) {
				for (Resource msg : model.filter(null, RDFS.SUBCLASSOF, res)
						.subjects()) {
					list.add(new RDFClass(model, msg));
				}
			}
		}
		return list;
	}

	public boolean isMessageClass() {
		return isMessage(this, new HashSet<RDFClass>());
	}

	public List<RDFProperty> getParameters() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
				if (prop instanceof URI) {
					set.add(prop.stringValue());
				}
			}
		}
		URI ont = model.filter(self, RDFS.ISDEFINEDBY, null).objectURI();
		for (Value sup : model.filter(self, RDFS.SUBCLASSOF, null).objects()) {
			if (model.contains((Resource) sup, RDFS.ISDEFINEDBY, ont)) {
				for (Resource prop : model.filter(null, RDFS.DOMAIN, sup)
						.subjects()) {
					if (!model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)) {
						if (prop instanceof URI) {
							set.add(prop.stringValue());
						}
					}
				}
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFProperty getResponseProperty() {
		RDFProperty obj = new RDFProperty(model, OBJ.OBJECT_RESPONSE);
		RDFProperty lit = new RDFProperty(model, OBJ.LITERAL_RESPONSE);
		boolean objUsed = false;
		boolean litUsed = false;
		boolean obj0 = false;
		boolean lit0 = false;
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
				BigInteger card = c.getBigInteger(OWL.CARDINALITY);
				BigInteger max = c.getBigInteger(OWL.MAXCARDINALITY);
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
					} else if (max != null && 0 == max.intValue()) {
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

	private boolean isMessage(RDFClass message, Set<RDFClass> set) {
		if (OBJ.MESSAGE.equals(message.getURI()))
			return true;
		set.add(message);
		for (RDFClass sup : message.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (!set.contains(sup) && isMessage(sup, set))
				return true;
		}
		return false;
	}

	private File createSourceFile(File dir, JavaNameResolver resolver) {
		String pkg = resolver.getPackageName(getURI());
		String simple = resolver.getSimpleName(getURI());
		File folder = dir;
		if (pkg != null) {
			folder = new File(dir, pkg.replace('.', '/'));
		}
		folder.mkdirs();
		return new File(folder, simple + ".java");
	}
}
