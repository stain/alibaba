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
import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.ArrayList;
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
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.rdf;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFOntology;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.vocabulary.OBJ;

public class JavaBuilder {
	private static final String MAP_STRING_OBJECT = "java.util.Map<java.lang.String, java.lang.Object>";
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final URI DATARANGE = new URIImpl(OWL.NAMESPACE
			+ "DataRange");
	private static final URI RESOURCE = RDFS.RESOURCE;
	private static final URI LITERAL = RDFS.LITERAL;
	private JavaClassBuilder out;
	private JavaNameResolver resolver;

	public JavaBuilder(JavaClassBuilder builder, JavaNameResolver resolver)
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

	public void packageInfo(RDFOntology ontology, String namespace)
			throws ObjectStoreConfigException {
		comment(out, ontology);
		annotationProperties(out, ontology);
		out.annotateStrings(rdf.class, singletonList(namespace));
		out.pkg(resolver.getPackageName(new URIImpl(namespace)));
	}

	public void interfaceHeader(RDFClass concept)
			throws ObjectStoreConfigException {
		String pkg = resolver.getPackageName(concept.getURI());
		String simple = resolver.getSimpleName(concept.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, concept);
		if (concept.isA(OWL.DEPRECATEDCLASS)) {
			out.annotate(Deprecated.class);
		}
		annotationProperties(out, concept);
		if (!resolver.isAnonymous(concept.getURI())) {
			out.annotateURI(rdf.class, resolver.getType(concept.getURI()));
		}
		out.interfaceName(simple);
		for (RDFClass sups : concept.getRDFClasses(RDFS.SUBCLASSOF)) {
			if (sups.getURI() == null || sups.equals(concept))
				continue;
			out.extend(resolver.getClassName(sups.getURI()));
		}
	}

	public void classHeader(RDFClass datatype)
			throws ObjectStoreConfigException {
		String pkg = resolver.getPackageName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, datatype);
		annotationProperties(out, datatype);
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

	public JavaBuilder classHeader(RDFProperty method)
			throws ObjectStoreConfigException {
		String pkg = resolver.getPackageName(method.getURI());
		String simple = resolver.getSimpleName(method.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		// some imports may not have rdf:type
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			if (imp.isA(OWL.CLASS)) {
				out.imports(resolver.getClassName(imp.getURI()));
			}
		}
		comment(out, method);
		annotationProperties(out, method);
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
		out.implement(RDFObject.class.getName());
		try {
			out.abstractMethod(Object.class.getMethod("equals", Object.class));
			out.abstractMethod(Object.class.getMethod("hashCode"));
			out.abstractMethod(Object.class.getMethod("toString"));
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
		return this;
	}

	public void annotationHeader(RDFProperty property)
			throws ObjectStoreConfigException {
		String pkg = resolver.getPackageName(property.getURI());
		String simple = resolver.getSimpleName(property.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, property);
		if (property.isA(OWL.DEPRECATEDPROPERTY)) {
			out.annotate(Deprecated.class);
		}
		annotationProperties(out, property);
		out.annotateURI(rdf.class, resolver.getType(property.getURI()));
		out.annotateEnum(Retention.class, RetentionPolicy.class, "RUNTIME");
		boolean valueOfClass = property.isClassDomain();
		if (valueOfClass) {
			out
					.annotateEnums(Target.class, ElementType.class, "TYPE",
							"METHOD");
		} else {
			out.annotateEnums(Target.class, ElementType.class, "TYPE",
					"METHOD", "PARAMETER", "ANNOTATION_TYPE", "PACKAGE");
		}
		out.annotationName(simple);
		if (valueOfClass && property.isA(OWL.FUNCTIONALPROPERTY)) {
			out.method("value").returnType(out.imports(Class.class)).end();
		} else if (valueOfClass) {
			out.method("value").returnType(out.imports(Class.class) + "[]")
					.end();
		} else if (property.isA(OWL.FUNCTIONALPROPERTY)) {
			out.method("value").returnType(out.imports(String.class)).end();
		} else {
			out.method("value").returnType(out.imports(String.class) + "[]")
					.end();
		}
	}

	public JavaBuilder constants(RDFClass concept) {
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

	public JavaBuilder stringConstructor(RDFClass datatype)
			throws ObjectStoreConfigException {
		String cn = resolver.getClassName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		JavaMethodBuilder method = out.staticMethod("valueOf");
		method.returnType(cn);
		method.param(null, String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
		JavaMethodBuilder code = out.constructor();
		code.param(null, String.class.getName(), "value");
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

	public JavaBuilder property(RDFClass dec, RDFProperty property)
			throws ObjectStoreConfigException {
		JavaPropertyBuilder prop = out.property(getPropertyName(dec, property));
		comment(prop, property);
		if (property.isA(OWL.DEPRECATEDPROPERTY)) {
			prop.annotate(Deprecated.class);
		}
		annotationProperties(prop, property);
		URI type = resolver.getType(property.getURI());
		prop.annotateURI(rdf.class, type);
		String className = getRangeClassName(dec, property);
		if (dec.isFunctional(property)) {
			prop.type(className);
		} else {
			prop.setOf(className);
		}
		prop.getter();
		if (!property.isReadOnly()) {
			comment(prop, property);
			if (property.isA(OWL.DEPRECATEDPROPERTY)) {
				prop.annotate(Deprecated.class);
			}
			annotationProperties(prop, property);
			prop.annotateURI(rdf.class, type);
			prop.setter();
		}
		prop.end();
		return this;
	}

	public JavaBuilder message(RDFClass code) throws ObjectStoreConfigException {
		return message(code, null, null);
	}

	public JavaBuilder message(RDFClass code, RDFProperty method, String body)
			throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(code.getURI());
		if (methodName.startsWith("get") && code.getParameters().isEmpty()) {
			return method(null, code, method, body);
		}
		if (methodName.startsWith("set") && code.getParameters().size() == 1) {
			return method(null, code, method, body);
		}
		if (methodName.startsWith("is") && code.getParameters().isEmpty()) {
			RDFProperty response = code.getResponseProperty();
			String range = getRangeClassName(code, response);
			if ("boolean".equals(range))
				return method(null, code, method, body);
		}
		// method name does not conflict with a property
		return method(code.getURI(), code, method, body);
	}

	public JavaBuilder method(URI uri, RDFClass receives, RDFProperty property,
			String body) throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(receives.getURI());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, receives);
		annotationProperties(method, receives);
		URI rdfType = resolver.getType(uri);
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
			URI pred = param.getURI();
			URI rdf = resolver.getType(pred);
			if (receives.isFunctional(param)) {
				String name = resolver.getMemberName(pred);
				method.param(rdf, type, name);
			} else {
				String name = resolver.getPluralPropertyName(pred);
				method.paramSetOf(rdf, type, name);
			}
		}
		if (body != null) {
			method(property, body, method);
		}
		method.end();
		return this;
	}

	public JavaBuilder methodAliasMap(RDFClass receives)
			throws ObjectStoreConfigException {
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
		method.param(null, MAP_STRING_OBJECT, "args");
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

	public JavaBuilder trigger(RDFProperty trigger, String body)
			throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(trigger.getURI());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, trigger);
		annotationProperties(method, trigger);
		List<URI> uris = new ArrayList<URI>();
		for (RDFProperty p : trigger.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (resolver.getType(p.getURI()) != null && !p.isTrigger()) {
				if (OBJ.DATATYPE_TRIGGER.equals(p.getURI()))
					continue;
				if (OBJ.OBJECT_TRIGGER.equals(p.getURI()))
					continue;
				uris.add(resolver.getType(p.getURI()));
			}
		}
		method.annotateURIs(triggeredBy.class, uris);
		method.returnType("void");
		for (RDFProperty param : trigger.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (resolver.getType(param.getURI()) != null && !param.isTrigger()) {
				if (OBJ.DATATYPE_TRIGGER.equals(param.getURI()))
					continue;
				if (OBJ.OBJECT_TRIGGER.equals(param.getURI()))
					continue;
				RDFClass domain = trigger.getRDFClass(RDFS.DOMAIN);
				String type = getRangeClassName(domain, param);
				URI pred = param.getURI();
				URI rdf = resolver.getType(pred);
				if (domain.isFunctional(param)) {
					String name = resolver.getMemberName(pred);
					method.param(rdf, type, name);
				} else {
					String name = resolver.getPluralPropertyName(pred);
					method.paramSetOf(rdf, type, name);
				}
			}
		}
		method(trigger, body, method);
		method.end();
		return this;
	}

	private void method(RDFProperty property, String body, JavaMethodBuilder out)
			throws ObjectStoreConfigException {
		out.code("try {\n\t\t\t");
		importVariables(out, property);
		out.code(body);
		out.code("\n\t\t} catch(");
		out.code(out.imports(Exception.class)).code(" e) {\n");
		out.code("\t\t\tthrow new ");
		out.code(out.imports(BehaviourException.class)).code("(e);\n");
		out.code("\t\t}\n");
	}

	private void importVariables(JavaMethodBuilder out, RDFProperty method)
			throws ObjectStoreConfigException {
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			URI subj = imp.getURI();
			if (!imp.isA(OWL.CLASS) && subj != null) {
				String name = resolver.getMemberName(subj);
				URI type = null;
				Model model = method.getModel();
				for (Value t : model.filter(subj, RDF.TYPE, null).objects()) {
					if (t instanceof URI
							&& (type == null || model.contains((URI) t,
									RDFS.SUBCLASSOF, type))) {
						type = (URI) t;
					}
				}
				String className = out.imports(resolver.getClassName(type));
				out.code(className);
				out.code(" ").code(name).code(" = (").code(className)
						.code(") ");
				out.code(GET_CONNECTION).code("().getObject(\"");
				out.code(subj.stringValue()).code("\");\n\t\t\t");
			}
		}
	}

	private void comment(JavaSourceBuilder out, RDFEntity concept)
			throws ObjectStoreConfigException {
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

	private void annotationProperties(JavaSourceBuilder out, RDFEntity entity)
			throws ObjectStoreConfigException {
		for (RDFProperty property : entity.getRDFProperties()) {
			if (property.isA(OWL.ANNOTATIONPROPERTY)) {
				String ann = resolver.getClassName(property.getURI());
				boolean valueOfClass = property.isClassDomain();
				if (valueOfClass && property.isA(OWL.FUNCTIONALPROPERTY)) {
					RDFClass value = entity.getRDFClass(property.getURI());
					String className = resolver.getClassName(value.getURI());
					out.annotateClass(ann, className);
				} else if (valueOfClass) {
					List<String> classNames = new ArrayList<String>();
					for (RDFClass value : entity.getRDFClasses(property
							.getURI())) {
						classNames.add(resolver.getClassName(value.getURI()));
					}
					out.annotateClasses(ann, classNames);
				} else if (property.isA(OWL.FUNCTIONALPROPERTY)) {
					out
							.annotateString(ann, entity.getString(property
									.getURI()));
				} else {
					out.annotateStrings(ann, entity.getStrings(property
							.getURI()));
				}
			}
		}
	}

	private String getPropertyName(RDFClass code, RDFProperty param) {
		if (code.isFunctional(param)) {
			return resolver.getMemberName(param.getURI());
		} else {
			return resolver.getPluralPropertyName(param.getURI());
		}
	}

	private String getObjectRangeClassName(RDFClass code, RDFProperty property)
			throws ObjectStoreConfigException {
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

	private String getRangeClassName(RDFClass code, RDFProperty property)
			throws ObjectStoreConfigException {
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
