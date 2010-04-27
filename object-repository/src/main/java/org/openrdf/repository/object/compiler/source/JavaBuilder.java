/*
 * Copyright (c) 2008-2010, Zepheira LLC Some rights reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.SPARQLQueryOptimizer;
import org.openrdf.repository.object.managers.helpers.XSLTOptimizer;
import org.openrdf.repository.object.managers.helpers.XSLTransformer;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Dumping ground for creating Java source files. TODO Needs to be split into
 * multiple classes.
 * 
 * @author James Leigh
 * 
 */
public class JavaBuilder extends JavaClassBuilder {
	private static final String MAP_STRING_OBJECT = "java.util.Map<java.lang.String, java.lang.Object>";
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final URI DATARANGE = new URIImpl(OWL.NAMESPACE
			+ "DataRange");
	private static final URI RESOURCE = RDFS.RESOURCE;
	private static final URI LITERAL = RDFS.LITERAL;
	private static final String JAVA_NS = "java:";
	private JavaNameResolver resolver;
	private Pattern startsWithPrefix = Pattern.compile("\\s*PREFIX\\s.*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static Set<String> parameterTypes;
	static {
		Set<String> set = new HashSet<String>();
		for (Method method : XSLTransformer.class.getMethods()) {
			if ("with".equals(method.getName())
					&& method.getParameterTypes().length == 2) {
				set.add(method.getParameterTypes()[1].getName());
			}
		}
		parameterTypes = Collections.unmodifiableSet(set);
	}

	public JavaBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source);
		this.resolver = resolver;
	}

	public String getMemberPrefix(String ns) {
		return resolver.getMemberPrefix(ns);
	}

	public String getPackageName(URI uri) {
		return resolver.getPackageName(uri);
	}

	public String getSimpleName(URI name) {
		return resolver.getSimpleName(name);
	}

	public URI getType(URI name) {
		return resolver.getType(name);
	}

	public boolean isAnonymous(URI name) {
		return resolver.isAnonymous(name);
	}

	public String getClassName(URI name) throws ObjectStoreConfigException {
		return resolver.getClassName(name);
	}

	public void comment(RDFEntity concept) throws ObjectStoreConfigException {
		comment(this, concept);
	}

	public void comment(JavaSourceBuilder out, RDFEntity concept)
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

	public void annotationProperties(RDFEntity entity)
			throws ObjectStoreConfigException {
		annotationProperties(this, entity, false);
	}

	public void annotationProperties(RDFEntity entity, boolean impls)
			throws ObjectStoreConfigException {
		annotationProperties(this, entity, impls);
	}

	public void annotationProperties(JavaSourceBuilder out, RDFEntity entity)
			throws ObjectStoreConfigException {
		annotationProperties(out, entity, false);
	}

	public String getPropertyName(RDFClass code, RDFProperty param) {
		if (code.isFunctional(param)) {
			return resolver.getMemberName(param.getURI());
		} else {
			return resolver.getPluralPropertyName(param.getURI());
		}
	}

	public String getRangeClassName(RDFClass code, RDFProperty property)
			throws ObjectStoreConfigException {
		String type = getRangeObjectClassName(code, property);
		if (code.isMinCardinality(property)) {
			type = unwrap(type);
		}
		return type;
	}

	public JavaMethodBuilder message(RDFClass msg, boolean isAbstract)
			throws ObjectStoreConfigException {
		URI uri = msg.getURI();
		if (isBeanProperty(msg)) {
			uri = null;
		}
		String methodName = resolver.getMethodName(msg.getURI());
		JavaMethodBuilder code = method(methodName, isAbstract);
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
		return code;
	}

	public JavaBuilder message(RDFClass msg, RDFClass method, String body)
			throws ObjectStoreConfigException {
		JavaMethodBuilder code = beginMethod(msg, body == null);
		method(method, body, code);
		code.end();
		return this;
	}

	public JavaBuilder methodAliasMap(RDFEntity receives)
			throws ObjectStoreConfigException {
		RDFClass code = (RDFClass) receives;
		String methodName = resolver.getMethodName(code.getURI());
		JavaMethodBuilder method = method(methodName, false);
		comment(method, receives);
		RDFProperty response = code.getResponseProperty();
		String range = getRangeClassName(code, response);
		if (code.isFunctional(response)) {
			method.returnType(range);
		} else {
			method.returnSetOf(range);
		}
		method.param(MAP_STRING_OBJECT, "args");
		method.code("try {\n");
		if (!"void".equals(range)) {
			method.code("return ");
		}
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
		method.code("\n\t\t} catch(");
		method.code(method.imports(RuntimeException.class)).code(" e) {\n");
		method.code("\t\t\tthrow e;");
		method.code("\n\t\t} catch(");
		method.code(method.imports(Exception.class)).code(" e) {\n");
		method.code("\t\t\tthrow new ");
		method.code(method.imports(BehaviourException.class)).code("(e, ");
		method.string(String.valueOf(code.getURI())).code(");\n");
		method.code("\t\t}\n");
		method.end();
		return this;
	}

	public JavaBuilder sparql(RDFClass msg, RDFClass property, String sparql,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		RDFProperty resp = msg.getResponseProperty();
		JavaMethodBuilder out = message(msg, sparql == null);
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
					boolean primitive = !getRangeObjectClassName(msg, param)
							.equals(getRangeClassName(msg, param));
					boolean bool = getRangeClassName(msg, param).equals(
							"boolean");
					parameters.put(name, getBindingValue(name, datatype,
							primitive, bool));
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of sparql methods must be functional: "
									+ property.getURI());
				}
			}
			out.code(new SPARQLQueryOptimizer().implementQuery(qry, base,
					eager, range, rangeClassName, functional, parameters));
			out.code("\n\t\t} catch(");
			out.code(out.imports(RuntimeException.class)).code(" e) {\n");
			out.code("\t\t\tthrow e;");
			out.code("\n\t\t} catch(");
			out.code(out.imports(Exception.class)).code(" e) {\n");
			out.code("\t\t\tthrow new ");
			out.code(out.imports(BehaviourException.class)).code("(e, ");
			out.string(String.valueOf(property.getURI())).code(");\n");
			out.code("\t\t}\n");
		}
		out.end();
		return this;
	}

	public JavaBuilder xslt(RDFClass msg, RDFClass property, String xslt,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		XSLTOptimizer optimizer = new XSLTOptimizer();
		RDFProperty resp = msg.getResponseProperty();
		String base = property.getURI().stringValue();
		String field = "xslt" + Math.abs(base.hashCode());
		staticField(imports(optimizer.getFieldType()), field, optimizer
				.getFieldConstructor(xslt, base));
		JavaMethodBuilder out = message(msg, xslt == null);
		if (xslt != null) {
			String rangeClassName = getRangeClassName(msg, resp);
			String input = null;
			String inputName = null;
			out.code("try {\n\t\t\t");
			Map<String, String> parameters = new HashMap<String, String>();
			for (RDFProperty param : msg.getParameters()) {
				if (msg.isFunctional(param)) {
					String name = resolver
							.getExplicitMemberName(param.getURI());
					String range = getRangeClassName(msg, param);
					if (name != null) {
						boolean datatype = msg.getRange(param).isDatatype();
						boolean primitive = !getRangeObjectClassName(msg, param)
								.equals(range);
						boolean bool = range.equals("boolean");
						if (parameterTypes.contains(range)) {
							parameters.put(name, name);
						} else {
							parameters.put(name, getBindingValue(name,
									datatype, primitive, bool)
									+ ".stringValue()");
						}
					} else {
						input = range;
						name = resolver.getMemberName(param.getURI());
						inputName = name;
					}
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of xslt methods must be functional: "
									+ property.getURI());
				}
			}
			out.code(optimizer.implementXSLT(field, input, inputName,
					parameters, rangeClassName));
			out.code("\n\t\t} catch(");
			out.code(out.imports(RuntimeException.class)).code(" e) {\n");
			out.code("\t\t\tthrow e;");
			out.code("\n\t\t} catch(");
			out.code(out.imports(Exception.class)).code(" e) {\n");
			out.code("\t\t\tthrow new ");
			out.code(out.imports(BehaviourException.class)).code("(e, ");
			out.string(String.valueOf(property.getURI())).code(");\n");
			out.code("\t\t}\n");
		}
		out.end();
		return this;
	}

	private String getBindingValue(String name, boolean datatype,
			boolean primitive, boolean bool) {
		StringBuilder out = new StringBuilder();
		if (bool || primitive) {
			out
					.append("getObjectConnection().getValueFactory().createLiteral(");
			out.append(name);
			out.append(")");
		} else if (datatype) {
			out.append(name).append(" == null ? null : ");
			out
					.append("getObjectConnection().getObjectFactory().createLiteral(");
			out.append(name);
			out.append(")");
		} else {
			out.append(name).append(" == null ? null : ");
			out.append("((");
			out.append(RDFObject.class.getName()).append(")");
			out.append(name);
			out.append(").getResource()");
		}
		return out.toString();
	}

	private String prefixQueryString(String sparql,
			Map<String, String> namespaces) {
		if (startsWithPrefix.matcher(sparql).matches())
			return sparql;
		String regex = "[pP][rR][eE][fF][iI][xX]\\s+";
		StringBuilder sb = new StringBuilder(256 + sparql.length());
		for (String prefix : namespaces.keySet()) {
			String pattern = regex + prefix + "\\s*:";
			Matcher m = Pattern.compile(pattern).matcher(sparql);
			if (sparql.contains(prefix) && !m.find()) {
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

	private JavaMethodBuilder beginMethod(RDFClass msg, boolean isAbstract)
			throws ObjectStoreConfigException {
		URI uri = msg.getURI();
		if (isBeanProperty(msg)) {
			uri = null;
		}
		String methodName = resolver.getMethodName(msg.getURI());
		JavaMethodBuilder code = method(methodName, isAbstract);
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

	private void method(RDFEntity property, String body, JavaMethodBuilder out)
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
		out.code(out.imports(BehaviourException.class)).code("(e, ");
		out.string(String.valueOf(property.getURI())).code(");\n");
		out.code("\t\t}\n");
	}

	private void importVariables(JavaMethodBuilder out, RDFEntity method)
			throws ObjectStoreConfigException {
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			URI subj = imp.getURI();
			if (!imp.isA(OWL.CLASS)
					&& !imp.getURI().getNamespace().equals(JAVA_NS)
					&& subj != null) {
				String name = var(resolver.getSimpleName(subj));
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

	private void annotationProperties(JavaSourceBuilder out, RDFEntity entity,
			boolean impls) throws ObjectStoreConfigException {
		loop: for (RDFProperty property : entity.getRDFProperties()) {
			URI iri = property.getURI();
			if (OBJ.MESSAGE_IMPLS.contains(iri))
				continue;
			boolean compiled = resolver.isCompiledAnnotation(iri);
			if (property.isA(OWL.ANNOTATIONPROPERTY) || compiled) {
				URI uri = resolver.getType(iri);
				String ann = resolver.getClassName(uri);
				boolean valueOfClass = property.isClassRange()
						|| resolver.isAnnotationOfClasses(uri);
				boolean functional = property.isA(OWL.FUNCTIONALPROPERTY);
				if (compiled && !functional) {
					functional = resolver.isCompiledAnnotationFunctional(iri);
				}
				if (valueOfClass && functional) {
					RDFClass value = entity.getRDFClass(uri);
					String className = resolver.getClassName(value.getURI());
					out.annotateClass(ann, className);
				} else if (valueOfClass) {
					List<String> classNames = new ArrayList<String>();
					for (RDFClass value : entity.getRDFClasses(uri)) {
						if (value.getURI() == null)
							continue loop;
						String cn = resolver.getClassName(value.getURI());
						if (impls && OBJ.PRECEDES.equals(uri)) {
							Set<String> strings = value
									.getStrings(OBJ.IMPL_NAME);
							if (strings.isEmpty()) {
								classNames.add(cn);
							} else {
								for (String suffix : strings) {
									classNames.add(cn + suffix);
								}
							}
						} else {
							classNames.add(cn);
						}
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
