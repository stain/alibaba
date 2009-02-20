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
package org.openrdf.elmo.codegen.builder;

import static java.util.Collections.singletonList;
import static org.openrdf.elmo.codegen.vocabulary.ELMO.METHOD;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.openrdf.concepts.owl.DataRange;
import org.openrdf.concepts.owl.DeprecatedClass;
import org.openrdf.concepts.owl.DeprecatedProperty;
import org.openrdf.concepts.owl.OwlProperty;
import org.openrdf.concepts.owl.Restriction;
import org.openrdf.concepts.owl.SymmetricProperty;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.concepts.rdfs.Resource;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.complementOf;
import org.openrdf.elmo.annotations.disjointWith;
import org.openrdf.elmo.annotations.intersectionOf;
import org.openrdf.elmo.annotations.inverseOf;
import org.openrdf.elmo.annotations.oneOf;
import org.openrdf.elmo.annotations.rdf;
import org.openrdf.elmo.codegen.JavaNameResolver;
import org.openrdf.elmo.codegen.concepts.CodeClass;
import org.openrdf.elmo.codegen.concepts.CodeMessageClass;
import org.openrdf.elmo.codegen.concepts.CodeOntology;
import org.openrdf.elmo.codegen.concepts.CodeProperty;
import org.openrdf.elmo.codegen.concepts.Method;
import org.openrdf.elmo.codegen.source.JavaClassBuilder;
import org.openrdf.elmo.codegen.source.JavaCommentBuilder;
import org.openrdf.elmo.codegen.source.JavaMethodBuilder;
import org.openrdf.elmo.codegen.source.JavaPropertyBuilder;
import org.openrdf.elmo.codegen.source.JavaSourceBuilder;
import org.openrdf.elmo.codegen.vocabulary.ELMO;

public class JavaCodeBuilder {
	public static final String INVOKE_SUFFIX = "$elmoInvoke";
	private static final String MAP_STRING_OBJECT = "java.util.Map<java.lang.String, java.lang.Object>";
	private static final String OWL = "http://www.w3.org/2002/07/owl#";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final QName NOTHING = new QName(OWL, "Nothing");
	private static final QName RESOURCE = new QName(RDFS, "Resource");
	private static final QName LITERAL = new QName(RDFS, "Literal");
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

	public void packageInfo(CodeOntology ontology, String namespace) {
		comment(out, ontology);
		out.annotateStrings(rdf.class, singletonList(namespace));
		out.pkg(resolver.getPackageName(new QName(namespace, "")));
	}

	public void interfaceHeader(CodeClass concept) {
		String pkg = resolver.getPackageName(concept.getQName());
		String simple = resolver.getSimpleName(concept.getQName());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, concept);
		if (concept instanceof DeprecatedClass) {
			out.annotate(Deprecated.class);
		}
		List<QName> list = new ArrayList<QName>();
		QName type = resolver.getType(concept.getQName());
		if (type != null) {
			list.add(type);
		}
		for (Class eq : concept.getOwlEquivalentClasses()) {
			type = resolver.getType(eq.getQName());
			if (type != null) {
				list.add(type);
			}
		}
		out.annotateQNames(rdf.class, list);
		List<QName> oneOf = new ArrayList<QName>();
		if (concept.getOwlOneOf() != null) {
			for (Object o : concept.getOwlOneOf()) {
				if (o instanceof Entity) {
					oneOf.add(((Entity) o).getQName());
				}
			}
		}
		out.annotateQNames(oneOf.class, oneOf);
		annotate(intersectionOf.class, concept.getOwlIntersectionOf());
		annotate(complementOf.class, concept.getOwlComplementOf());
		annotate(disjointWith.class, concept.getOwlDisjointWith());
		out.interfaceName(simple);
		for (Class sups : concept.getRdfsSubClassOf()) {
			if (sups.getQName() == null || sups.equals(concept))
				continue;
			out.extend(resolver.getClassName(sups.getQName()));
		}
	}

	public void classHeader(CodeClass datatype) {
		String pkg = resolver.getPackageName(datatype.getQName());
		String simple = resolver.getSimpleName(datatype.getQName());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, datatype);
		QName type = resolver.getType(datatype.getQName());
		out.annotateQName(rdf.class, type);
		out.className(simple);
		for (Class sups : datatype.getRdfsSubClassOf()) {
			if (sups.getQName() == null || sups.equals(datatype))
				continue;
			out.extend(resolver.getClassName(sups.getQName()));
		}
	}

	public JavaCodeBuilder invokeClassHeader(CodeMessageClass msg) {
		String pkg = resolver.getPackageName(msg.getQName());
		String simple = resolver.getSimpleName(msg.getQName()) + INVOKE_SUFFIX;
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, msg);
		out.abstractName(simple);
		out.implement(resolver.getClassName(msg.getQName()));
		return this;
	}

	public JavaCodeBuilder classHeader(Method method) {
		String pkg = resolver.getPackageName(method.getQName());
		String simple = resolver.getSimpleName(method.getQName());
		if (pkg != null) {
			out.pkg(pkg);
		}
		// some imports may not have rdf:type
		Set<? extends Entity> imports = method.getElmoImports();
		for (Entity imp : imports) {
			out.imports(resolver.getClassName(imp.getQName()));
		}
		comment(out, method);
		out.abstractName(simple);
		if (method.getElmoSubMethodOf() != null) {
			QName name = method.getElmoSubMethodOf().getQName();
			if (!METHOD.equals(name)) {
				out.extend(resolver.getClassName(name));
			}
		}
		Class domain = method.getElmoDomain();
		if (domain.getQName() != null) {
			out.implement(resolver.getClassName(domain.getQName()));
		}
		return this;
	}

	public JavaCodeBuilder constants(CodeClass concept) {
		List<Object> oneOf = concept.getOwlOneOf();
		if (oneOf != null) {
			List<String> names = new ArrayList<String>(oneOf.size());
			for (Object one : oneOf) {
				if (one instanceof Entity) {
					QName qname = ((Entity) one).getQName();
					String localPart = qname.getLocalPart();
					String name = localPart.replaceAll("^[^a-zA-Z]", "_").replaceAll("\\W", "_").toUpperCase();
					names.add(name);
					out.staticQNameField(name, qname);
				}
			}
			if (!names.isEmpty()) {
				out.staticQNameArrayField("QNAMES", names);
			}
		}
		return this;
	}

	public JavaCodeBuilder stringConstructor(CodeClass datatype) {
		String cn = resolver.getClassName(datatype.getQName());
		String simple = resolver.getSimpleName(datatype.getQName());
		JavaMethodBuilder method = out.staticMethod("valueOf");
		method.returnType(cn);
		method.param(String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
		JavaMethodBuilder code = out.constructor();
		code.param(String.class.getName(), "value");
		code.code("super(value);").end();
		return this;
	}

	public JavaCodeBuilder constructor(Method method) {
		String face = Entity.class.getName();
		if (method.getElmoDomain() != null) {
			Entity domain = method.getElmoDomain();
			face = resolver.getClassName(domain.getQName());
		}
		out.field(face, "self");
		JavaMethodBuilder code = out.constructor();
		code.param(Entity.class.getName(), "self");
		if (method.getElmoSubMethodOf() != null) {
			if (!METHOD.equals(method.getElmoSubMethodOf().getQName())) {
				code.code("super(self);\n");
			}
		}
		code.code("this.self = (").code(code.imports(face));
		code.code(") self;").end();
		return this;
	}

	public JavaCodeBuilder property(CodeClass dec, Property property) {
		JavaPropertyBuilder prop = out.property(getPropertyName(dec, property));
		comment(prop, property);
		if (property instanceof DeprecatedProperty) {
			prop.annotate(Deprecated.class);
		}
		List<QName> list = new ArrayList<QName>();
		QName type = resolver.getType(property.getQName());
		if (type != null) {
			list.add(type);
		}
		if (property instanceof OwlProperty) {
			OwlProperty p = (OwlProperty) property;
			for (Property eq : p.getOwlEquivalentProperties()) {
				type = resolver.getType(eq.getQName());
				if (type != null) {
					list.add(type);
				}
			}
		}
		prop.annotateQNames(rdf.class, list);
		list.clear();
		if (property instanceof SymmetricProperty) {
			type = resolver.getType(property.getQName());
			if (type != null) {
				list.add(type);
			}
		}
		if (property instanceof CodeProperty) {
			CodeProperty p = (CodeProperty) property;
			for (Property eq : p.findAllInverseOfProperties()) {
				type = resolver.getType(eq.getQName());
				if (type != null) {
					list.add(type);
				}
			}
		}
		prop.annotateQNames(inverseOf.class, list);
		String className = getRangeClassName(dec, property);
		if (dec.isFunctional(property)) {
			prop.type(className);
		} else {
			prop.setOf(className);
		}
		prop.getter();
		comment(prop, property);
		CodeClass range = dec.getRange(property);
		if (range instanceof DataRange) {
			List<Object> oneOf = range.getOwlOneOf();
			int size = oneOf.size();
			if (size > 0) {
				Object first = oneOf.get(0);
				if (first instanceof Entity) {
					Entity[] ar = new Entity[size];
					prop.annotateEntities(oneOf.class, oneOf.toArray(ar));
				} else if (first instanceof String) {
					String[] ar = new String[size];
					prop.annotateLabels(oneOf.class, oneOf.toArray(ar));
				} else {
					List<String> labels = new ArrayList<String>(size);
					for (Object o : oneOf) {
						labels.add(o.toString());
					}
					prop.annotateLabels(oneOf.class, labels, range.toString());
				}
			}
		}
		prop.end();
		return this;
	}

	public JavaCodeBuilder message(CodeMessageClass code) {
		String methodName = resolver.getMethodName(code.getQName());
		if (methodName.startsWith("get") && code.getParameters().isEmpty()) {
			return method(null, code, null);
		}
		if (methodName.startsWith("is") && code.getParameters().isEmpty()) {
			Property response = code.getResponseProperty();
			String range = getRangeClassName(code, response);
			if ("boolean".equals(range))
				return method(null, code, null);
		}
		// method name does not conflict with a property
		return method(code.getQName(), code, null);
	}

	public JavaCodeBuilder invokeMethod(CodeMessageClass msg) {
		String methodName = resolver.getMethodName(ELMO.INVOKE);
		JavaMethodBuilder method = out.method(methodName);
		comment(method, msg);
		method.returnSetOf(Object.class.getName());
		Property target = (Property) msg.getElmoManager().find(ELMO.TARGET);
		Property response = msg.getResponseProperty();
		String range = getRangeClassName(msg, response);
		if (!msg.isFunctional(response)) {
			method.code("return ");
		} else if (!"void".equals(range)) {
			method.code("return ").code(method.imports(Collections.class));
			method.code(".singleton((").code(method.imports(Object.class)).code(") ");
		}
		method.code("((").code(method.imports(getRangeClassName(msg, target))).code(") ");
		method.code(getGetterMethod(msg, target)).code("()).");
		method.code(resolver.getMethodName(msg.getQName())).code("(");
		Iterator<Property> iter = msg.getParameters().iterator();
		while (iter.hasNext()) {
			method.code(getGetterMethod(msg, iter.next())).code("()");
			if (iter.hasNext()) {
				method.code(", ");
			}
		}
		if ("void".equals(range)) {
			method.code(");\n");
			method.code("\t\treturn null;");
		} else if (msg.isFunctional(response)) {
			method.code("));");
		} else { 
			method.code(");");
		}
		method.end();
		return this;
	}

	public JavaCodeBuilder method(QName qname, Class receives, String body) {
		CodeMessageClass code = (CodeMessageClass) receives;
		String methodName = resolver.getMethodName(code.getQName());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, receives);
		QName rdfType = resolver.getType(qname);
		if (rdfType != null) {
			method.annotateQName(rdf.class, rdfType);
		}
		Property response = code.getResponseProperty();
		String range = getRangeClassName(code, response);
		if (code.isFunctional(response)) {
			method.returnType(range);
		} else {
			method.returnSetOf(range);
		}
		Iterator<Property> iter = code.getParameters().iterator();
		while (iter.hasNext()) {
			Property param = iter.next();
			String type = getRangeClassName(code, param);
			if (code.isFunctional(param)) {
				String name = resolver.getPropertyName(param.getQName());
				method.param(type, name);
			} else {
				String name = resolver.getPluralPropertyName(param.getQName());
				method.paramSetOf(type, name);
			}
		}
		method.code(body);
		method.end();
		return this;
	}

	public JavaCodeBuilder methodAliasMap(
			org.openrdf.concepts.owl.Class receives) {
		CodeMessageClass code = (CodeMessageClass) receives;
		String methodName = resolver.getMethodName(code.getQName());
		JavaMethodBuilder method = out.method(methodName);
		comment(method, receives);
		Property response = code.getResponseProperty();
		String range = getRangeClassName(code, response);
		if (code.isFunctional(response)) {
			method.returnType(range);
		} else {
			method.returnSetOf(range);
		}
		method.param(MAP_STRING_OBJECT, "args");
		method.code(methodName);
		method.code("(");
		Iterator<Property> iter = code.getParameters().iterator();
		while (iter.hasNext()) {
			Property param = iter.next();
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

	private void comment(JavaSourceBuilder out, Resource concept) {
		JavaCommentBuilder comment = out.comment(concept.getRdfsComment());
		for (Object see : concept.getRdfsSeeAlso()) {
			if (see instanceof Class) {
				QName name = ((Class) see).getQName();
				comment.seeAlso(resolver.getClassName(name));
			} else if (see instanceof Property) {
				Property property = (Property) see;
				for (Class domain : property.getRdfsDomains()) {
					CodeClass cc = (CodeClass) domain;
					String cn = resolver.getClassName(domain.getQName());
					String name = getPropertyName(cc, property);
					String range = getRangeClassName(cc, property);
					if ("boolean".equals(range)) {
						comment.seeBooleanProperty(cn, name);
					} else {
						comment.seeProperty(cn, name);
					}
				}
			} else {
				comment.seeAlso(see.toString());
			}
		}
		if (concept instanceof Thing) {
			for (Object version : ((Thing) concept).getOwlVersionInfo()) {
				comment.version(version.toString());
			}
		}
		comment.end();
	}

	private void annotate(java.lang.Class<?> ann,
			Collection<? extends Class> list) {
		if (list != null && !list.isEmpty()) {
			List<String> classes = new ArrayList<String>();
			for (Class c : list) {
				if (c instanceof Restriction)
					return;
				classes.add(resolver.getClassName(c.getQName()));
			}
			out.annotateClasses(ann, classes);
		}
	}

	private void annotate(java.lang.Class<?> ann, Class complement) {
		if (complement != null) {
			String className = resolver.getClassName(complement.getQName());
			out.annotateClass(ann, className);
		}
	}

	private String getGetterMethod(CodeClass code, Property param) {
		String className = getRangeClassName(code, param);
		String property = getPropertyName(code, param);
		String cap = property.substring(0, 1).toUpperCase();
		String rest = property.substring(1);
		if ("boolean".equals(className)) {
			return "is" + cap + rest;
		} else {
			return "get" + cap + rest;
		}
	}

	private String getPropertyName(CodeClass code, Property param) {
		if (code.isFunctional(param)) {
			return resolver.getPropertyName(param.getQName());
		} else {
			return resolver.getPluralPropertyName(param.getQName());
		}
	}

	private String getObjectRangeClassName(CodeClass code, Property property) {
		CodeClass range = code.getRange(property);
		if (range == null)
			return Object.class.getName();
		if (range instanceof DataRange) {
			String type = null;
			for (Object data : range.getOwlOneOf()) {
				type = data.getClass().getName();
			}
			return type;
		} else if (NOTHING.equals(range.getQName())) {
			return "void";
		} else if (LITERAL.equals(range.getQName())) {
			return Object.class.getName();
		} else if (RESOURCE.equals(range.getQName())) {
			return Object.class.getName();
		} else if (range.getQName() != null) {
			return resolver.getClassName(range.getQName());
		} else {
			return Object.class.getName();
		}
	}

	private String getRangeClassName(CodeClass code, Property property) {
		CodeClass range = code.getRange(property);
		if (range == null)
			return Object.class.getName();
		String type = null;
		if (range instanceof DataRange) {
			for (Object data : range.getOwlOneOf()) {
				type = data.getClass().getName();
			}
		} else if (NOTHING.equals(range.getQName())) {
			return "void";
		} else if (LITERAL.equals(range.getQName())) {
			return Object.class.getName();
		} else if (RESOURCE.equals(range.getQName())) {
			return Object.class.getName();
		} else if (range.getQName() != null) {
			type = resolver.getClassName(range.getQName());
		} else {
			return Object.class.getName();
		}
		BigInteger one = BigInteger.valueOf(1);
		for (Class c : code.getRdfsSubClassOf()) {
			if (c instanceof Restriction) {
				Restriction r = (Restriction) c;
				if (property.equals(r.getOwlOnProperty())) {
					if (one.equals(r.getOwlMaxCardinality())
							&& one.equals(r.getOwlMinCardinality())
							|| one.equals(r.getOwlCardinality())) {
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
