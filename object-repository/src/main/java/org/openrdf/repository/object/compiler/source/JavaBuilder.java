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

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.annotations.prefix;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.triggeredBy;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFOntology;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.ObjectQueryOptimizer;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Dumping ground for creating Java source files. TODO Needs to be split into
 * multiple classes.
 * 
 * @author James Leigh
 * 
 */
public class JavaBuilder {
	private static final String MAP_STRING_OBJECT = "java.util.Map<java.lang.String, java.lang.Object>";
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final URI DATARANGE = new URIImpl(OWL.NAMESPACE
			+ "DataRange");
	private static final URI RESOURCE = RDFS.RESOURCE;
	private static final URI LITERAL = RDFS.LITERAL;
	private static final String JAVA_NS = "java:";
	private JavaClassBuilder out;
	private JavaNameResolver resolver;
	private Pattern startsWithPrefix = Pattern.compile("\\s*PREFIX", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
		out.annotateString(prefix.class.getName(), resolver.getMemberPrefix(namespace));
		out.annotateString(iri.class.getName(), namespace);
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
			out.annotateURI(iri.class, resolver.getType(concept.getURI()));
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
		out.annotateURI(iri.class, type);
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
			if (imp.isA(OWL.CLASS) || imp.getURI().getNamespace().equals(JAVA_NS)) {
				out.imports(resolver.getClassName(imp.getURI()));
			}
		}
		comment(out, method);
		annotationProperties(out, method);
		out.abstractName(simple);
		List<URI> supers = new ArrayList<URI>();
		for (RDFProperty p : method.getRDFProperties(RDFS.SUBPROPERTYOF)) {
			if (p.isMethodOrTrigger()) {
				supers.add(p.getURI());
			}
		}
		if (supers.size() == 1) {
			out.extend(resolver.getClassName(supers.get(0)));
		}
		RDFClass domain = method.getRDFClass(RDFS.DOMAIN);
		if (domain == null || RDFS.RESOURCE.equals(domain.getURI())) {
			domain = method.getRDFClass(RDFS.RANGE).getRange(OBJ.TARGET);
		}
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
		out.annotateURI(iri.class, resolver.getType(property.getURI()));
		out.annotateEnum(Retention.class, RetentionPolicy.class, "RUNTIME");
		boolean valueOfClass = property.isClassRange();
		if (property.isClassDomain()) {
			out
					.annotateEnums(Target.class, ElementType.class, "TYPE",
							"METHOD");
		} else {
			out.annotateEnums(Target.class, ElementType.class, "TYPE",
					"METHOD", "PARAMETER", "ANNOTATION_TYPE", "PACKAGE");
		}
		out.annotationName(simple);
		if (valueOfClass && property.isA(OWL.FUNCTIONALPROPERTY)) {
			out.method("value", true).returnType(out.imports(Class.class)).end();
		} else if (valueOfClass) {
			out.method("value", true).returnType(out.imports(Class.class) + "[]")
					.end();
		} else if (property.isA(OWL.FUNCTIONALPROPERTY)) {
			out.method("value", true).returnType(out.imports(String.class)).end();
		} else {
			out.method("value", true).returnType(out.imports(String.class) + "[]")
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
		method.param(String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
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
			JavaMethodBuilder code = out.constructor();
			code.param(String.class.getName(), "value");
			code.code("super(value);");
			code.end();
		} else {
			out.field(String.class.getName(), "value");
			JavaMethodBuilder code = out.constructor();
			code.param(String.class.getName(), "value");
			code.code("this.value = value;");
			code.end();
			code = out.method("toString", false).returnType(String.class.getName());
			code.code("return value;").end();
			code = out.method("hashCode", false).returnType("int");
			code.code("return value.hashCode();").end();
			code = out.method("equals", false).returnType("boolean");
			code.param(Object.class.getName(), "o");
			String equals = "return getClass().equals(o.getClass()) && toString().equals(o.toString());";
			code.code(equals).end();
		}
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
		prop.annotateURI(iri.class, type);
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
			prop.annotateURI(iri.class, type);
			prop.setter();
		}
		prop.end();
		return this;
	}

	public JavaBuilder message(RDFClass msg) throws ObjectStoreConfigException {
		URI uri = msg.getURI();
		if (isBeanProperty(msg)) {
			uri = null;
		}
		String methodName = resolver.getMethodName(msg.getURI());
		JavaMethodBuilder code = out.method(methodName, true);
		comment(code, msg);
		annotationProperties(code, msg);
		URI rdfType = resolver.getType(uri);
		if (rdfType != null) {
			code.annotateURI(iri.class, rdfType);
		}
		RDFProperty response = msg.getResponseProperty();
		String range = getRangeClassName(msg, response);
		if (msg.isFunctional(response)) {
			code.returnType(range);
		} else {
			code.returnSetOf(range);
		}
		for (RDFProperty param : msg.getParameters()) {
			String type = getRangeClassName(msg, param);
			URI pred = param.getURI();
			URI rdf = resolver.getType(pred);
			annotationProperties(code, param);
			if (rdf != null) {
				code.annotateURI(iri.class, rdf);
			}
			if (msg.isFunctional(param)) {
				String name = resolver.getMemberName(pred);
				code.param(type, name);
			} else {
				String name = resolver.getPluralPropertyName(pred);
				code.paramSetOf(type, name);
			}
		}
		code.end();
		return this;
	}

	public JavaBuilder message(RDFClass msg, RDFProperty method, String body)
			throws ObjectStoreConfigException {
		URI uri = msg.getURI();
		if (isBeanProperty(msg)) {
			uri = null;
		}
		JavaMethodBuilder code = beginMethod(uri, msg, method, body == null);
		method(method, body, code);
		code.end();
		return this;
	}

	public JavaBuilder methodAliasMap(RDFClass receives)
			throws ObjectStoreConfigException {
		RDFClass code = (RDFClass) receives;
		String methodName = resolver.getMethodName(code.getURI());
		JavaMethodBuilder method = out.method(methodName, false);
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

	public JavaBuilder trigger(RDFProperty trigger, String body)
			throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(trigger.getURI());
		JavaMethodBuilder method = out.method(methodName, body == null);
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
				method.annotateURI(iri.class, rdf);
				if (domain.isFunctional(param)) {
					String name = resolver.getMemberName(pred);
					method.param(type, name);
				} else {
					String name = resolver.getPluralPropertyName(pred);
					method.paramSetOf(type, name);
				}
			}
		}
		method(trigger, body, method);
		method.end();
		return this;
	}

	public JavaBuilder sparql(RDFClass msg, RDFProperty property,
			String sparql, Map<String, String> namespaces)
			throws ObjectStoreConfigException {
		URI uri = isBeanProperty(msg) ? null : msg.getURI();
		RDFProperty resp = msg.getResponseProperty();
		JavaMethodBuilder out = beginMethod(uri, msg, property, sparql == null);
		if (sparql != null) {
			String range = getRangeObjectClassName(msg, resp);
			String rangeClassName = getRangeClassName(msg, resp);
			RDFClass range2 = msg.getRange(resp);
			Map<String, String> eager = null;
			if (!range2.isDatatype()) {
				eager = new HashMap<String, String>();
				for (RDFProperty prop : range2
						.getFunctionalDatatypeProperties()) {
					URI p = resolver.getType(prop.getURI());
					String name = resolver.getMemberName(p);
					eager.put(name, p.stringValue());
				}
			}
			String qry = prefixQueryString(sparql, namespaces);
			out.code("try {\n\t\t\t");
			String base = property.getURI().stringValue();
			boolean functional = msg.isFunctional(resp);
			Map<String, String> parameters = new HashMap<String, String>();
			for (RDFProperty param : msg.getParameters()) {
				if (msg.isFunctional(param)) {
					String name = resolver.getMemberName(param.getURI());
					boolean datatype = msg.getRange(param).isDatatype();
					boolean primitive = !range.equals(rangeClassName);
					boolean bool = rangeClassName.equals("boolean");
					parameters.put(name, getBindingValue(name, datatype, primitive, bool));
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of sparql methods must be functional: "
									+ property.getURI());
				}
			}
			out.code(new ObjectQueryOptimizer().implementQuery(qry, base,
					eager, range, rangeClassName, functional, parameters));
			out.code("\n\t\t} catch(");
			out.code(out.imports(RuntimeException.class)).code(" e) {\n");
			out.code("\t\t\tthrow e;");
			out.code("\n\t\t} catch(");
			out.code(out.imports(Exception.class)).code(" e) {\n");
			out.code("\t\t\tthrow new ");
			out.code(out.imports(BehaviourException.class)).code("(");
			out.string(String.valueOf(property.getURI())).code(", e);\n");
			out.code("\t\t}\n");
		}
		out.end();
		return this;
	}

	private String getBindingValue(String name, boolean datatype, boolean primitive, boolean bool) {
		StringBuilder out = new StringBuilder();
		String cap = name.substring(0, 1).toUpperCase();
		if (bool) {
			out.append("getObjectConnection().getValueFactory().createLiteral(");
			out.append("msg.is").append(cap).append(name.substring(1));
			out.append("())");
		} else if (primitive) {
			out.append("getObjectConnection().getValueFactory().createLiteral(");
			out.append("msg.get").append(cap).append(name.substring(1));
			out.append("())");
		} else if (datatype) {
			out.append("getObjectConnection().getObjectFactory().createLiteral(");
			out.append("msg.get").append(cap).append(name.substring(1));
			out.append("())");
		} else {
			out.append("((");
			out.append(RDFObject.class.getName()).append(")");
			out.append("msg.get").append(cap).append(name.substring(1));
			out.append("()).getResource()");
		}
		return out.toString();
	}

	private String prefixQueryString(String sparql, Map<String, String> namespaces) {
		if (startsWithPrefix.matcher(sparql).matches())
			return sparql;
		StringBuilder sb = new StringBuilder(256 + sparql.length());
		for (String prefix : namespaces.keySet()) {
			if (sparql.contains(prefix)) {
				sb.append("PREFIX ").append(prefix).append(":<");
				sb.append(namespaces.get(prefix)).append("> ");
			}
		}
		return sb.append(sparql).toString();
	}

	private boolean isBeanProperty(RDFClass code)
			throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(code.getURI());
		if (methodName.startsWith("get") && code.getParameters().isEmpty()) {
			return true;
		}
		if (methodName.startsWith("set") && code.getParameters().size() == 1) {
			return true;
		}
		if (methodName.startsWith("is") && code.getParameters().isEmpty()) {
			RDFProperty response = code.getResponseProperty();
			String range = getRangeClassName(code, response);
			if ("boolean".equals(range))
				return true;
		}
		return false;
	}

	private JavaMethodBuilder beginMethod(URI uri, RDFClass msg, RDFProperty method, boolean isAbstract)
			throws ObjectStoreConfigException {
		String methodName = resolver.getMethodName(msg.getURI());
		JavaMethodBuilder code = out.method(methodName, isAbstract);
		comment(code, msg);
		annotationProperties(code, msg);
		URI rdfType = resolver.getType(uri);
		if (rdfType != null) {
			code.annotateURI(iri.class, rdfType);
		}
		List<String> parameters = new ArrayList<String>();
		for (RDFProperty param : msg.getParameters()) {
			if (msg.isFunctional(param)) {
				parameters.add(getRangeClassName(msg, param));
			} else {
				parameters.add(Set.class.getName());
			}
		}
		code.annotateClasses(parameterTypes.class.getName(), parameters);
		RDFProperty response = msg.getResponseProperty();
		String range = getRangeClassName(msg, response);
		if (msg.isFunctional(response)) {
			code.returnType(range);
		} else {
			code.returnSetOf(range);
		}
		code.param(resolver.getClassName(msg.getURI()), "msg");
		return code;
	}

	private void method(RDFProperty property, String body, JavaMethodBuilder out)
			throws ObjectStoreConfigException {
		out.code("try {\n\t\t\t");
		importVariables(out, property);
		out.code(body);
		out.code("\n\t\t} catch(");
		out.code(out.imports(RuntimeException.class)).code(" e) {\n");
		out.code("\t\t\tthrow e;");
		out.code("\n\t\t} catch(");
		out.code(out.imports(Exception.class)).code(" e) {\n");
		out.code("\t\t\tthrow new ");
		out.code(out.imports(BehaviourException.class)).code("(");
		out.string(String.valueOf(property.getURI())).code(", e);\n");
		out.code("\t\t}\n");
	}

	private void importVariables(JavaMethodBuilder out, RDFProperty method)
			throws ObjectStoreConfigException {
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			URI subj = imp.getURI();
			if (!imp.isA(OWL.CLASS) && !imp.getURI().getNamespace().equals(JAVA_NS) && subj != null) {
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
			boolean compiled = resolver.isCompiledAnnotation(property.getURI());
			if (property.isA(OWL.ANNOTATIONPROPERTY) || compiled) {
				URI uri = resolver.getType(property.getURI());
				String ann = resolver.getClassName(uri);
				boolean valueOfClass = property.isClassRange()
						|| resolver.isAnnotationOfClasses(uri);
				boolean functional = property.isA(OWL.FUNCTIONALPROPERTY);
				if (valueOfClass && functional) {
					RDFClass value = entity.getRDFClass(uri);
					String className = resolver.getClassName(value.getURI());
					out.annotateClass(ann, className);
				} else if (valueOfClass) {
					List<String> classNames = new ArrayList<String>();
					for (RDFClass value : entity.getRDFClasses(uri)) {
						classNames.add(resolver.getClassName(value.getURI()));
					}
					out.annotateClasses(ann, classNames);
				} else if (functional) {
					out.annotateString(ann, entity.getString(uri));
				} else {
					out.annotateStrings(ann, entity.getStrings(uri));
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

	private String getRangeObjectClassName(RDFClass code, RDFProperty property)
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
		return type;
	}

	private String getRangeClassName(RDFClass code, RDFProperty property)
			throws ObjectStoreConfigException {
		String type = getRangeObjectClassName(code, property);
		if (code.isMinCardinality(property)) {
			type = unwrap(type);
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
