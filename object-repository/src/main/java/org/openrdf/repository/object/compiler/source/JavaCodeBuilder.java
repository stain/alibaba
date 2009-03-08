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
package org.openrdf.repository.object.compiler.source;

import static java.util.Collections.singletonList;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.complementOf;
import org.openrdf.repository.object.annotations.intersectionOf;
import org.openrdf.repository.object.annotations.localized;
import org.openrdf.repository.object.annotations.oneOf;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFOntology;
import org.openrdf.repository.object.compiler.model.RDFProperty;

public class JavaCodeBuilder {
	private static final String MAP_STRING_OBJECT = "java.util.Map<java.lang.String, java.lang.Object>";
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final URI DATARANGE = new URIImpl(OWL.NAMESPACE
			+ "DataRange");
	private static final URI RESOURCE = RDFS.RESOURCE;
	private static final URI LITERAL = RDFS.LITERAL;
	private JavaClassBuilder out;
	private JavaNameResolver resolver;

	public JavaCodeBuilder(JavaClassBuilder builder, JavaNameResolver resolver)
			throws FileNotFoundException {
		this.out = builder;
		this.resolver = resolver;
	}

	public void setGroovy(boolean groovy) {
		out.setGroovy(groovy);
	}

	public void close() {
		out.close();
	}

	public void packageInfo(RDFOntology ontology, String namespace) {
		comment(out, ontology);
		out.annotateStrings(rdf.class, singletonList(namespace));
		out.pkg(resolver.getPackageName(new URIImpl(namespace)));
	}

	public void interfaceHeader(RDFClass concept) {
		String pkg = resolver.getPackageName(concept.getURI());
		String simple = resolver.getSimpleName(concept.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, concept);
		if (concept.isA(OWL.DEPRECATEDCLASS)) {
			out.annotate(Deprecated.class);
		}
		if (!resolver.isAnonymous(concept.getURI())) {
			out.annotateURI(rdf.class, resolver.getType(concept.getURI()));
		}
		if (resolver.isAnonymous(concept.getURI())) {
			List<URI> oneOf = new ArrayList<URI>();
			if (concept.getList(OWL.ONEOF) != null) {
				for (Value o : concept.getList(OWL.ONEOF)) {
					if (o instanceof URI) {
						oneOf.add((URI) o);
					}
				}
			}
			out.annotateURIs(oneOf.class, oneOf);
			annotate(intersectionOf.class, concept
					.getRDFClasses(OWL.INTERSECTIONOF));
			annotate(complementOf.class, concept
					.getRDFClasses(OWL.COMPLEMENTOF));
		}
		out.interfaceName(simple);
		for (RDFClass sups : concept.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(concept))
				continue;
			out.extend(resolver.getClassName(sups.getURI()));
		}
	}

	public void classHeader(RDFClass datatype) {
		String pkg = resolver.getPackageName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, datatype);
		URI type = resolver.getType(datatype.getURI());
		out.annotateURI(rdf.class, type);
		out.className(simple);
		for (RDFClass sups : datatype.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(datatype))
				continue;
			// rdfs:Literal rdfs:subClassOf rdfs:Resource
			if (!sups.isDatatype())
				continue;
			out.extend(resolver.getClassName(sups.getURI()));
		}
	}

	public JavaCodeBuilder classHeader(RDFProperty method) {
		String pkg = resolver.getPackageName(method.getURI());
		String simple = resolver.getSimpleName(method.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		// some imports may not have rdf:type
		Set<? extends RDFEntity> imports = method
				.getRDFClasses(org.openrdf.repository.object.vocabulary.OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			out.imports(resolver.getClassName(imp.getURI()));
		}
		comment(out, method);
		out.abstractName(simple);
		for (Value obj : method.getValues(RDFS.SUBPROPERTYOF)) {
			if (obj instanceof URI
					&& new RDFProperty(method.getModel(), (URI) obj)
							.isMethodOrTrigger()) {
				out.extend(resolver.getClassName((URI) obj));
			}
		}
		RDFClass domain = method.getRDFClass(RDFS.DOMAIN);
		if (domain != null && domain.getURI() != null) {
			out.implement(resolver.getClassName(domain.getURI()));
		}
		return this;
	}

	public JavaCodeBuilder constants(RDFClass concept) {
		List<? extends Value> oneOf = concept.getList(OWL.ONEOF);
		if (oneOf != null) {
			List<String> names = new ArrayList<String>();
			for (Value one : oneOf) {
				if (one instanceof URI) {
					URI uri = (URI) one;
					String localPart = uri.getLocalName();
					String name = localPart.replaceAll("^[^a-zA-Z]", "_")
							.replaceAll("\\W", "_").toUpperCase();
					names.add(name);
					out.staticURIField(name, uri);
				}
			}
			if (!names.isEmpty()) {
				out.staticURIArrayField("URIS", names);
			}
		}
		return this;
	}

	public JavaCodeBuilder stringConstructor(RDFClass datatype) {
		String cn = resolver.getClassName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		JavaMethodBuilder method = out.staticMethod("valueOf");
		method.returnType(cn);
		method.param(String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
		JavaMethodBuilder code = out.constructor();
		code.param(String.class.getName(), "value");
		boolean child = false;
		for (RDFClass sups : datatype.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(datatype))
				continue;
			// rdfs:Literal rdfs:subClassOf rdfs:Resource
			if (!sups.isDatatype())
				continue;
			child = true;
		}
		if (child) {
			code.code("super(value);");
		} else {
			// TODO rdfs:Literal
			code.code("super();");
		}
		code.end();
		return this;
	}

	public JavaCodeBuilder property(RDFClass dec, RDFProperty property) {
		JavaPropertyBuilder prop = out.property(getPropertyName(dec, property));
		comment(prop, property);
		if (property.isA(OWL.DEPRECATEDPROPERTY)) {
			prop.annotate(Deprecated.class);
		}
		URI type = resolver.getType(property.getURI());
		prop.annotateURI(rdf.class, type);
		String className = getRangeClassName(dec, property);
		if (dec.isFunctional(property)) {
			prop.type(className);
		} else {
			prop.setOf(className);
		}
		if (property.isLocalized()) {
			prop.annotate(localized.class);
		}
		prop.getter();
		comment(prop, property);
		prop.end();
		return this;
	}

	public JavaCodeBuilder message(RDFClass code) {
		String methodName = resolver.getMethodName(code.getURI());
		if (methodName.startsWith("get") && code.getParameters().isEmpty()) {
			return method(null, code, null);
		}
		if (methodName.startsWith("is") && code.getParameters().isEmpty()) {
			RDFProperty response = code.getResponseProperty();
			String range = getRangeClassName(code, response);
			if ("boolean".equals(range))
				return method(null, code, null);
		}
		// method name does not conflict with a property
		return method(code.getURI(), code, null);
	}

	public JavaCodeBuilder method(URI URI, RDFClass receives, String body) {
		String methodName = resolver.getMethodName(receives.getURI());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, receives);
		URI rdfType = resolver.getType(URI);
		if (rdfType != null) {
			method.annotateURI(rdf.class, rdfType);
		}
		RDFProperty response = receives.getResponseProperty();
		String range = getRangeClassName(receives, response);
		if (receives.isFunctional(response)) {
			method.returnType(range);
		} else {
			method.returnSetOf(range);
		}
		Iterator<RDFProperty> iter = receives.getParameters().iterator();
		while (iter.hasNext()) {
			RDFProperty param = iter.next();
			String type = getRangeClassName(receives, param);
			if (receives.isFunctional(param)) {
				String name = resolver.getPropertyName(param.getURI());
				method.param(type, name);
			} else {
				String name = resolver.getPluralPropertyName(param.getURI());
				method.paramSetOf(type, name);
			}
		}
		method.code(body);
		method.end();
		return this;
	}

	public JavaCodeBuilder methodAliasMap(RDFClass receives) {
		RDFClass code = (RDFClass) receives;
		String methodName = resolver.getMethodName(code.getURI());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, receives);
		RDFProperty response = code.getResponseProperty();
		String range = getRangeClassName(code, response);
		if (code.isFunctional(response)) {
			method.returnType(range);
		} else {
			method.returnSetOf(range);
		}
		method.param(MAP_STRING_OBJECT, "args");
		method.code(methodName);
		method.code("(");
		Iterator<RDFProperty> iter = code.getParameters().iterator();
		while (iter.hasNext()) {
			RDFProperty param = iter.next();
			method.code("(");
			if (code.isFunctional(param)) {
				method.code(getObjectRangeClassName(code, param));
			} else {
				method.code(method.imports(Set.class));
			}
			method.code(") ");
			String name = getPropertyName(code, param);
			method.code("args.get(\"");
			method.code(name);
			method.code("\")");
			if (iter.hasNext()) {
				method.code(", ");
			}
		}
		method.code(");");
		method.end();
		return this;
	}

	public JavaCodeBuilder trigger(RDFProperty trigger, String body) {
		String methodName = resolver.getMethodName(trigger.getURI());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, trigger);
		List<URI> uris = new ArrayList<URI>();
		for (RDFProperty p : trigger.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (p.getURI() != null && !p.isTrigger()) {
				uris.add(p.getURI());
			}
		}
		method.annotateURIs(triggeredBy.class, uris);
		method.returnType("void");
		method.code(body);
		method.end();
		return this;
	}

	private void comment(JavaSourceBuilder out, RDFEntity concept) {
		StringBuilder sb = new StringBuilder();
		for (Value obj : concept.getValues(RDFS.COMMENT)) {
			sb.append(obj.stringValue()).append("\n");
		}
		JavaCommentBuilder comment = out.comment(sb.toString().trim());
		for (Value see : concept.getValues(RDFS.SEEALSO)) {
			Model model = concept.getModel();
			if (see instanceof URI
					&& model.contains((URI) see, RDF.TYPE, OWL.CLASS)) {
				comment.seeAlso(resolver.getClassName((URI) see));
			} else if (see instanceof URI
					&& model.contains((URI) see, RDF.TYPE, RDF.PROPERTY)) {
				RDFProperty property = new RDFProperty(model, (URI) see);
				for (RDFClass domain : property.getRDFClasses(RDFS.DOMAIN)) {
					RDFClass cc = (RDFClass) domain;
					String cn = resolver.getClassName(domain.getURI());
					String name = getPropertyName(cc, property);
					String range = getRangeClassName(cc, property);
					if ("boolean".equals(range)) {
						comment.seeBooleanProperty(cn, name);
					} else {
						comment.seeProperty(cn, name);
					}
				}
			} else {
				comment.seeAlso(see.stringValue());
			}
		}
		if (concept instanceof RDFEntity) {
			for (Object version : ((RDFEntity) concept)
					.getStrings(OWL.VERSIONINFO)) {
				comment.version(version.toString());
			}
		}
		comment.end();
	}

	private void annotate(java.lang.Class<?> ann,
			Collection<? extends RDFClass> list) {
		if (list != null && !list.isEmpty()) {
			List<String> classes = new ArrayList<String>();
			for (RDFClass c : list) {
				if (c.isA(OWL.RESTRICTION))
					return;
				classes.add(resolver.getClassName(c.getURI()));
			}
			out.annotateClasses(ann, classes);
		}
	}

	private String getPropertyName(RDFClass code, RDFProperty param) {
		if (code.isFunctional(param)) {
			return resolver.getPropertyName(param.getURI());
		} else {
			return resolver.getPluralPropertyName(param.getURI());
		}
	}

	private String getObjectRangeClassName(RDFClass code, RDFProperty property) {
		RDFClass range = code.getRange(property);
		if (range == null)
			return Object.class.getName();
		if (range.isA(DATARANGE)) {
			String type = null;
			for (Value value : range.getList(OWL.ONEOF)) {
				URI datatype = ((Literal) value).getDatatype();
				if (datatype == null) {
					type = String.class.getName();
				} else {
					type = resolver.getClassName(datatype);
				}
			}
			return type;
		} else if (NOTHING.equals(range.getURI())) {
			return "void";
		} else if (LITERAL.equals(range.getURI())) {
			return Object.class.getName();
		} else if (RESOURCE.equals(range.getURI())) {
			return Object.class.getName();
		} else if (range.getURI() != null) {
			return resolver.getClassName(range.getURI());
		} else {
			return Object.class.getName();
		}
	}

	private String getRangeClassName(RDFClass code, RDFProperty property) {
		RDFClass range = code.getRange(property);
		if (range == null)
			return Object.class.getName();
		String type = null;
		if (range.isA(DATARANGE)) {
			for (Value value : range.getList(OWL.ONEOF)) {
				URI datatype = ((Literal) value).getDatatype();
				if (datatype == null) {
					type = String.class.getName();
				} else {
					type = resolver.getClassName(datatype);
				}
			}
		} else if (NOTHING.equals(range.getURI())) {
			return "void";
		} else if (LITERAL.equals(range.getURI())) {
			return Object.class.getName();
		} else if (RESOURCE.equals(range.getURI())) {
			return Object.class.getName();
		} else if (range.getURI() != null) {
			type = resolver.getClassName(range.getURI());
		} else {
			return Object.class.getName();
		}
		BigInteger one = BigInteger.valueOf(1);
		for (RDFClass c : code.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (c.isA(OWL.RESTRICTION)) {
				if (property.equals(c.getRDFProperty(OWL.ONPROPERTY))) {
					if (one.equals(c.getBigInteger(OWL.MAXCARDINALITY))
							&& one.equals(c.getBigInteger(OWL.MINCARDINALITY))
							|| one.equals(c.getBigInteger(OWL.CARDINALITY))) {
						type = unwrap(type);
					}
				}
			}
		}
		return type;
	}

	private String unwrap(String type) {
		if (type.equals("java.lang.Character"))
			return "char";
		if (type.equals("java.lang.Byte"))
			return "byte";
		if (type.equals("java.lang.Short"))
			return "short";
		if (type.equals("java.lang.Integer"))
			return "int";
		if (type.equals("java.lang.Long"))
			return "long";
		if (type.equals("java.lang.Float"))
			return "float";
		if (type.equals("java.lang.Double"))
			return "double";
		if (type.equals("java.lang.Boolean"))
			return "boolean";
		return type;
	}

}
