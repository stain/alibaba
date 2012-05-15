/*
 * Copyright (c) 2008-2010, Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.annotations.Iri;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.RDFList;
import org.openrdf.repository.object.compiler.source.JavaMessageBuilder;
import org.openrdf.repository.object.compiler.source.JavaMethodBuilder;
import org.openrdf.repository.object.compiler.source.JavaPropertyBuilder;
import org.openrdf.repository.object.compiler.source.JavaScriptBuilder;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.openrdf.repository.object.vocabulary.MSG;

/**
 * Helper object for traversing the OWL model.
 * 
 * @author James Leigh
 * 
 */
public class RDFClass extends RDFEntity {
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");

	public RDFClass(Model model, Resource self) {
		super(model, self);
	}

	public boolean isDatatype() {
		if (self instanceof URI
				&& XMLDatatypeUtil.isBuiltInDatatype((URI) self))
			return true;
		if (self.equals(RDFS.LITERAL))
			return true;
		if (self instanceof URI) {
			URI uri = (URI) self;
			if (uri.getNamespace().equals(RDF.NAMESPACE)
					&& uri.getLocalName().equals("PlainLiteral"))
				return true;
		}
		return isA(RDFS.DATATYPE);
	}

	public RDFClass getRange(URI pred) {
		return getRange(new RDFProperty(model, pred));
	}

	public RDFClass getRange(RDFProperty property) {
		return getRange(property, true);
	}

	public RDFClass getRange(RDFProperty property, boolean convariant) {
		RDFClass type = getRangeOrNull(property, convariant);
		if (type == null)
			return new RDFClass(getModel(), RDFS.RESOURCE);
		return type;
	}

	private RDFClass getRangeOrNull(RDFProperty property, boolean convariant) {
		if (convariant) {
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
		}
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION) || c.equals(this))
				continue;
			RDFClass type = ((RDFClass) c).getRangeOrNull(property, convariant);
			if (type != null) {
				return type;
			}
		}
		for (RDFClass r : property.getRDFClasses(RDFS.RANGE)) {
			return r;
		}
		for (RDFProperty p : property.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			RDFClass superRange = getRangeOrNull(p, convariant);
			if (superRange != null) {
				return superRange;
			}
		}
		return null;
	}

	public boolean isFunctional(RDFProperty property) {
		return isFunctionalProperty(property);
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

	public List<RDFProperty> getParameters() {
		TreeSet<String> set = new TreeSet<String>();
		addParameters(set, new HashSet<Value>());
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	public RDFProperty getResponseProperty() {
		Set<RDFProperty> set = new HashSet<RDFProperty>();
		set.add(new RDFProperty(model, MSG.OBJECT_SET));
		set.add(new RDFProperty(model, MSG.LITERAL_SET));
		set.add(new RDFProperty(model, MSG.OBJECT));
		set.add(new RDFProperty(model, MSG.LITERAL));
		for (RDFClass c : getRestrictions()) {
			RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
			String valuesFrom = c.getString(OWL.ALLVALUESFROM);
			if (RDFS.RESOURCE.stringValue().equals(valuesFrom))
				continue;
			if (NOTHING.stringValue().equals(valuesFrom))
				continue;
			BigInteger card = c.getBigInteger(OWL.CARDINALITY);
			if (card != null && 0 == card.intValue())
				continue;
			BigInteger max = c.getBigInteger(OWL.MAXCARDINALITY);
			if (max != null && 0 == max.intValue())
				continue;
			if (set.contains(property))
				return property;
		}
		for (RDFClass c : getRestrictions()) {
			RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
			String valuesFrom = c.getString(OWL.ALLVALUESFROM);
			if (RDFS.RESOURCE.stringValue().equals(valuesFrom))
				continue;
			if (set.contains(property))
				return property;
		}
		for (RDFClass c : getRestrictions()) {
			RDFProperty property = c.getRDFProperty(OWL.ONPROPERTY);
			if (set.contains(property))
				return property;
		}
		if (getString(MSG.TYPE) == null)
			return new RDFProperty(model, MSG.OBJECT_SET);
		return new RDFProperty(model, MSG.OBJECT); // @type implies functional
	}

	public boolean isMinCardinality(RDFProperty property) {
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							&& one.equals(c.getBigInteger(OWL.MINCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						return true;
					}
				}
			} else if (equals(c)) {
				continue;
			} else if (c.isMinCardinality(property)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty(JavaNameResolver resolver) {
		Collection<RDFProperty> properties = getDeclaredProperties();
		if (properties.size() > 1)
			return false;
		if (!properties.isEmpty()) {
			URI uri = properties.iterator().next().getURI();
			if (!MSG.TARGET.equals(uri))
				return false;
		}
		if (!getDeclaredMessages(resolver).isEmpty())
			return false;
		// TODO check annotations
		return false;
	}

	public File generateSourceCode(File dir, JavaNameResolver resolver)
			throws Exception {
		File source = createSourceFile(dir, resolver);
		if (isDatatype()) {
			JavaMessageBuilder builder = new JavaMessageBuilder(source, resolver);
			String pkg = resolver.getPackageName(this.getURI());
			String simple = resolver.getSimpleName(getURI());
			if (pkg == null) {
				builder.imports(simple);
			} else {
				builder.pkg(pkg);
				builder.imports(pkg + '.' + simple);
			}
			classHeader(simple, builder);
			stringConstructor(builder);
			builder.close();
		} else {
			JavaMessageBuilder builder = new JavaMessageBuilder(source, resolver);
			interfaceHeader(builder);
			constants(builder);
			for (RDFProperty prop : getDeclaredProperties()) {
				property(builder, prop);
			}
			for (RDFClass type : getDeclaredMessages(resolver)) {
				builder.message(type, true).end();
			}
			builder.close();
		}
		return source;
	}

	public List<RDFProperty> getFunctionalDatatypeProperties() {
		List<RDFProperty> list = new ArrayList<RDFProperty>();
		for (RDFProperty property : getProperties()) {
			if (isFunctional(property) && getRange(property).isDatatype()) {
				list.add(property);
			}
		}
		return list;
	}

	public boolean precedes(RDFClass p) {
		return model.contains(self, MSG.PRECEDES, p.self)
				|| model.contains(self, RDFS.SUBCLASSOF, p.self);
	}

	public Collection<RDFClass> getDeclaredMessages(JavaNameResolver resolver) {
		Set<RDFClass> set = new TreeSet<RDFClass>();
		for (Resource res : model.filter(null, OWL.ALLVALUESFROM, self)
				.subjects()) {
			if (model.contains(res, OWL.ONPROPERTY, MSG.TARGET)) {
				for (Resource msg : model.filter(null, RDFS.SUBCLASSOF, res)
						.subjects()) {
					RDFClass rc = new RDFClass(model, msg);
					if (rc.isMessageClass(resolver)) {
						set.add(rc);
					}
				}
			}
		}
		return set;
	}

	public Collection<RDFProperty> getDeclaredProperties() {
		TreeSet<String> set = new TreeSet<String>();
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (prop instanceof URI) {
				set.add(prop.stringValue());
			}
		}
		for (RDFClass res : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (res.isA(OWL.RESTRICTION)) {
				RDFProperty prop = res.getRDFProperty(OWL.ONPROPERTY);
				if (isFunctional(prop) == isFunctionalProperty(prop)) {
					set.add(prop.getURI().stringValue());
				}
			}
		}
		List<RDFProperty> list = new ArrayList<RDFProperty>(set.size());
		for (String uri : set) {
			list.add(new RDFProperty(model, new URIImpl(uri)));
		}
		return list;
	}

	/**
	 * Compiles the method into a collection of classes and resource stored in
	 * the given directory.
	 * 
	 * @param resolver
	 *            utility class to look up corresponding Java names
	 * @param namespaces
	 *            prefix -&gt; namespace
	 * @param dir
	 *            target directory of byte-code
	 * @return the full class name of the created role
	 * @throws Exception
	 */
	public Set<String> msgWriteSource(JavaNameResolver resolver,
			Map<String, String> namespaces, File dir) throws Exception {
		Set<String> result = new HashSet<String>();
		String pkg = resolver.getPackageName(this.getURI());
		Collection<URI> messageImpls = new ArrayList<URI>();
		messageImpls.addAll(MSG.MESSAGE_IMPLS);
		for (Statement st : getStatements(messageImpls)) {
			URI lang = st.getPredicate();
			String code = st.getObject().stringValue();
			String simple = resolver.getSimpleImplName(this.getURI(), code);
			File pkgDir = dir;
			if (pkg != null) {
				pkgDir = new File(dir, pkg.replace('.', '/'));
			}
			pkgDir.mkdirs();
			if (MSG.SCRIPT.equals(lang)) {
				File source = new File(pkgDir, simple + ".java");
				JavaScriptBuilder builder = new JavaScriptBuilder(source, resolver);
				if (pkg == null) {
					builder.imports(simple);
				} else {
					builder.pkg(pkg);
					builder.imports(pkg + '.' + simple);
				}
				classHeader(simple, builder);
				builder.engine(simple, this, code, namespaces);
				for (RDFClass msg : getMessages(resolver)) {
					builder.script(msg, this, code, namespaces);
				}
				builder.close();
				String name = simple;
				if (pkg != null) {
					name = pkg + '.' + simple;
				}
				result.add(name);
			}
		}
		return result;
	}

	protected Collection<RDFClass> getRestrictions() {
		Collection<RDFClass> restrictions = new LinkedHashSet<RDFClass>();
		for (RDFClass c : getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				restrictions.add(c);
			} else if (equals(c)) {
				continue;
			} else {
				restrictions.addAll(c.getRestrictions());
			}
		}
		return restrictions;
	}

	protected boolean isFunctionalProperty(RDFProperty property) {
		if (property.isA(OWL.FUNCTIONALPROPERTY))
			return true;
		URI uri = property.getURI();
		if (uri.equals(MSG.TARGET)
				|| uri.equals(MSG.LITERAL)
				|| uri.equals(MSG.OBJECT))
			return true;
		if (!property.getStrings(MSG.TYPE).isEmpty()) {
			// @type w/o range implies functional
			Set<? extends Value> range = property.getValues(RDFS.RANGE);
			if (range.isEmpty())
				return true;
			if (range.size() == 1 && range.contains(RDFS.RESOURCE))
				return true;
		}
		return false;
	}

	private void interfaceHeader(JavaMessageBuilder builder)
			throws ObjectStoreConfigException {
		String pkg = builder.getPackageName(this.getURI());
		String simple = builder.getSimpleName(this.getURI());
		if (pkg == null) {
			builder.imports(simple);
		} else {
			builder.pkg(pkg);
			builder.imports(pkg + '.' + simple);
		}
		builder.comment(this);
		if (this.isA(OWL.DEPRECATEDCLASS)) {
			builder.annotate(Deprecated.class);
		}
		builder.annotationProperties(this);
		if (!builder.isAnonymous(this.getURI())) {
			builder.annotateURI(Iri.class, builder.getType(this.getURI()));
		}
		builder.interfaceName(simple);
		for (RDFClass sups : this.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(this))
				continue;
			builder.extend(builder.getClassName(sups.getURI()));
		}
	}

	private void constants(JavaMessageBuilder builder) {
		List<? extends Value> oneOf = this.getList(OWL.ONEOF);
		if (oneOf != null) {
			Map<String, URI> names = new LinkedHashMap<String, URI>();
			for (Value one : oneOf) {
				if (one instanceof URI) {
					URI uri = (URI) one;
					String localPart = uri.getLocalName();
					if (localPart.length() < 1) {
						localPart = uri.stringValue();
					}
					String name = localPart.replaceAll("^[^a-zA-Z]", "_")
							.replaceAll("\\W+", "_").toUpperCase();
					if (names.containsKey(name)) {
						int count = 1;
						while (names.containsKey(name + '_' + count)) {
							count++;
						}
						name = name + '_' + count;
					}
					names.put(name, uri);
				}
			}
			if (!names.isEmpty()) {
				for (Map.Entry<String, URI> e : names.entrySet()) {
					builder.staticURIField(e.getKey(), e.getValue());
				}
				builder.staticURIArrayField("URIS", names.keySet());
			}
		}
	}

	private void stringConstructor(JavaMessageBuilder builder)
			throws ObjectStoreConfigException {
		String cn = builder.getClassName(this.getURI());
		String simple = builder.getSimpleName(this.getURI());
		JavaMethodBuilder method = builder.staticMethod("valueOf");
		method.returnType(cn);
		method.param(String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
		boolean child = false;
		for (RDFClass sups : this.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(this))
				continue;
			// rdfs:Literal rdfs:subClassOf rdfs:Resource
			if (!sups.isDatatype())
				continue;
			child = true;
		}
		if (child) {
			JavaMethodBuilder code = builder.constructor();
			code.param(String.class.getName(), "value");
			code.code("super(value);");
			code.end();
		} else {
			builder.field(String.class.getName(), "value");
			JavaMethodBuilder code = builder.constructor();
			code.param(String.class.getName(), "value");
			code.code("this.value = value;");
			code.end();
			code = builder.method("toString", false).returnType(
					String.class.getName());
			code.code("return value;").end();
			code = builder.method("hashCode", false).returnType("int");
			code.code("return value.hashCode();").end();
			code = builder.method("equals", false).returnType("boolean");
			code.param(Object.class.getName(), "o");
			String equals = "return getClass().equals(o.getClass()) && toString().equals(o.toString());";
			code.code(equals).end();
		}
	}

	private void property(JavaMessageBuilder builder, RDFProperty prop)
			throws ObjectStoreConfigException {
		JavaPropertyBuilder prop1 = builder.property(builder.getPropertyName(
				this, prop));
		builder.comment(prop1, prop);
		if (prop.isA(OWL.DEPRECATEDPROPERTY)) {
			prop1.annotate(Deprecated.class);
		}
		builder.annotationProperties(prop1, prop);
		URI type = builder.getType(prop.getURI());
		prop1.annotateURI(Iri.class, type);
		String className = builder.getPropertyClassName(this, prop);
		if (this.isFunctional(prop)) {
			prop1.type(className);
		} else {
			prop1.setOf(className);
		}
		prop1.getter();
		builder.comment(prop1, prop);
		if (prop.isA(OWL.DEPRECATEDPROPERTY)) {
			prop1.annotate(Deprecated.class);
		}
		builder.annotationProperties(prop1, prop);
		prop1.annotateURI(Iri.class, type);
		prop1.openSetter();
		builder.annotationProperties(prop1, prop);
		prop1.closeSetter();
		prop1.end();
	}

	private List<? extends RDFClass> getClassList(URI pred) {
		List<? extends Value> list = getList(pred);
		if (list == null)
			return Collections.emptyList();
		List<RDFClass> result = new ArrayList<RDFClass>();
		for (Value value : list) {
			if (value instanceof Resource) {
				Resource subj = (Resource) value;
				result.add(new RDFClass(model, subj));
			}
		}
		return result;
	}

	private void addParameters(Set<String> parameters, Set<Value> skip) {
		for (Resource prop : model.filter(null, RDFS.DOMAIN, self).subjects()) {
			if (isParameter(prop)) {
				parameters.add(prop.stringValue());
			}
		}
		for (Value sup : model.filter(self, RDFS.SUBCLASSOF, null).objects()) {
			if (isRDFSOrOWL(sup) || !skip.add(sup))
				continue;
			new RDFClass(model, (Resource) sup).addParameters(parameters, skip);
		}
	}

	private boolean isRDFSOrOWL(Value sup) {
		if (self instanceof URI && sup instanceof URI) {
			String ns = ((URI) self).getNamespace();
			return ns.equals(RDF.NAMESPACE) || ns.equals(RDFS.NAMESPACE)
					|| ns.equals(OWL.NAMESPACE);
		}
		return false;
	}

	private boolean isParameter(Resource prop) {
		return !model.contains(prop, RDF.TYPE, OWL.ANNOTATIONPROPERTY)
				&& prop instanceof URI
				&& !prop.stringValue().startsWith(MSG.NAMESPACE);
	}

	private boolean isMessageClass(JavaNameResolver resolver) {
		if (resolver.isAnonymous(getURI()))
			return false;
		if (getList(OWL.INTERSECTIONOF) != null)
			return false;
		return isMessage(this, new HashSet<RDFEntity>());
	}

	private boolean isMessage(RDFEntity message, Set<RDFEntity> set) {
		if (MSG.MESSAGE.equals(message.getURI()))
			return true;
		set.add(message);
		for (RDFClass sup : message.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (!set.contains(sup) && isMessage(sup, set))
				return true;
		}
		return false;
	}

	private BigInteger getBigInteger(URI pred) {
		Value value = model.filter(self, pred, null).objectValue();
		if (value == null)
			return null;
		return new BigInteger(value.stringValue());
	}

	private RDFProperty getRDFProperty(URI pred) {
		Resource subj = model.filter(self, pred, null).objectResource();
		if (subj == null)
			return null;
		return new RDFProperty(model, subj);
	}

	private Collection<RDFProperty> getProperties() {
		return getProperties(new HashSet<Resource>(),
				new ArrayList<RDFProperty>());
	}

	private Collection<RDFProperty> getProperties(Set<Resource> exclude,
			Collection<RDFProperty> list) {
		if (exclude.add(getResource())) {
			list.addAll(getDeclaredProperties());
			for (RDFClass sup : getRDFClasses(RDFS.SUBCLASSOF)) {
				list = sup.getProperties(exclude, list);
			}
		}
		return list;
	}

	private void classHeader(String simple, JavaMessageBuilder builder)
			throws ObjectStoreConfigException {
		builder.comment(this);
		if (this.isDatatype()) {
			builder.annotationProperties(this);
			URI type = builder.getType(this.getURI());
			builder.annotateURI(Iri.class, type);
			builder.className(simple);
		} else {
			builder.annotationProperties(this, true);
			builder.abstractName(simple);
		}
		if (this.isDatatype()) {
			List<URI> supers = new ArrayList<URI>();
			for (RDFClass sups : this.getRDFClasses(RDFS.SUBCLASSOF)) {
				if (sups.getURI() == null || sups.equals(this))
					continue;
				// rdfs:Literal rdfs:subClassOf rdfs:Resource
				if (!sups.isDatatype())
					continue;
				supers.add(sups.getURI());
			}
			if (supers.size() == 1) {
				builder.extend(builder.getClassName(supers.get(0)));
			}
		}
		if (!this.isDatatype()) {
			URI range = this.getRange(MSG.TARGET).getURI();
			if (range != null) {
				builder.implement(builder.getClassName(range));
			}
			builder.implement(RDFObject.class.getName());
			builder.implement(RDFObjectBehaviour.class.getName());
		}
	}

	private List<RDFClass> getMessages(JavaNameResolver resolver) {
		List<RDFClass> list = new ArrayList<RDFClass>();
		if (isMessageClass(resolver)) {
			list.add((RDFClass) this);
		}
		for (RDFClass msg : getClassList(OWL.INTERSECTIONOF)) {
			if (msg.isMessageClass(resolver)) {
				list.add(msg);
			}
		}
		for (RDFClass msg : getRDFClasses(OWL.EQUIVALENTCLASS)) {
			if (msg.isMessageClass(resolver)) {
				list.add(msg);
			}
		}
		return list;
	}
}
