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
package org.openrdf.repository.object.compiler.source;

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.parameterTypes;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.vocabulary.OBJ;

/**
 * Adds methods for implementing messages.
 * 
 * @author James Leigh
 * 
 */
public class JavaMessageBuilder extends JavaAnnotationBuilder {
	private static final String JAVA_NS = "java:";

	public JavaMessageBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
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
		String range = getResponseClassName(msg, response);
		if (msg.isFunctional(response)) {
			code.returnType(range);
		} else {
			code.returnSetOf(range);
		}
		for (RDFProperty param : msg.getParameters()) {
			String type = getParameterClassName(msg, param);
			URI pred = param.getURI();
			URI rdf = resolver.getType(pred);
			annotationProperties(code, param);
			if (rdf != null) {
				code.annotateURI(iri.class, rdf);
			}
			if (msg.isFunctional(param)) {
				String name = resolver.getSingleParameterName(pred);
				code.param(type, name);
			} else {
				String name = resolver.getPluralParameterName(pred);
				code.paramSetOf(type, name);
			}
		}
		return code;
	}

	public JavaMessageBuilder message(RDFClass msg, RDFClass method,
			boolean importVariables, String body)
			throws ObjectStoreConfigException {
		JavaMethodBuilder code = beginMethod(msg, body == null);
		method(method, importVariables, body, code);
		code.end();
		return this;
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
			String range = getResponseClassName(code, response);
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
				parameters.add(getParameterClassName(msg, param));
			} else {
				parameters.add(Set.class.getName());
			}
		}
		code.annotateClasses(parameterTypes.class.getName(), parameters);
		RDFProperty response = msg.getResponseProperty();
		String range = getResponseClassName(msg, response);
		if (msg.isFunctional(response)) {
			code.returnType(range);
		} else {
			code.returnSetOf(range);
		}
		code.param(resolver.getClassName(msg.getURI()), "msg");
		return code;
	}

	private void method(RDFEntity property, boolean importVaribales, String body, JavaMethodBuilder out)
			throws ObjectStoreConfigException {
		out.code("try {\n\t\t\t");
		if (importVaribales) {
			importVariables(out, property);
		}
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
		Set<RDFClass> imports = method.getRDFClasses(MSG.IMPORTS);
		imports.addAll(method.getRDFClasses(OBJ.IMPORTS));
		for (RDFEntity imp : imports) {
			URI subj = imp.getURI();
			if (!imp.getURI().getNamespace().equals(JAVA_NS)
					&& !imp.isA(OWL.CLASS)
					&& subj != null) {
				String name = var(resolver.getSimpleName(subj));
				URI type = null;
				Model model = method.getModel();
				for (Value t : model.filter(subj, RDF.TYPE, null).objects()) {
					if (t instanceof URI
							&& model.contains((URI) t, null, null)
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
}
