/*
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.SPARQLQueryOptimizer;

/**
 * Creates Java source code from a msg:sparql triples.
 *
 * @author James Leigh
 **/
public class JavaSparqlBuilder extends JavaMessageBuilder {

	public JavaSparqlBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
	}

	public JavaMessageBuilder sparql(RDFClass msg, RDFClass property, String sparql,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		SPARQLQueryOptimizer optimizer = new SPARQLQueryOptimizer();
		RDFProperty resp = msg.getResponseProperty();
		String base = property.getURI().stringValue();
		String field = "sparql" + Math.abs(msg.getURI().stringValue().hashCode());
		String fieldConstructor = optimizer.getFieldConstructor(sparql, base, namespaces);
		staticField(imports(optimizer.getFieldType()), field, fieldConstructor);
		JavaMethodBuilder out = message(msg, sparql == null);
		if (sparql != null) {
			String range = getRangeClassName(msg, resp);
			RDFClass range2 = msg.getRange(resp);
			Map<String, String> eager = null;
			if (range2 != null && !range2.isDatatype()) {
				eager = new HashMap<String, String>();
				for (RDFProperty prop : range2
						.getFunctionalDatatypeProperties()) {
					URI p = resolver.getType(prop.getURI());
					String name = resolver.getMemberName(p);
					eager.put(name, p.stringValue());
				}
			}
			out.code("try {\n\t\t\t");
			boolean functional = msg.isFunctional(resp);
			Map<String, String> params = new HashMap<String, String>();
			for (RDFProperty param : msg.getParameters()) {
				if (msg.isFunctional(param)) {
					String name = resolver.getMemberName(param.getURI());
					boolean datatype = msg.getRange(param).isDatatype();
					boolean primitive = !getRangeObjectClassName(msg, param)
							.equals(getRangeClassName(msg, param));
					boolean bool = getRangeClassName(msg, param).equals(
							"boolean");
					params.put(name, getBindingValue(name, datatype,
							primitive, bool));
				} else {
					// TODO handle plural parameterTypes
					throw new ObjectStoreConfigException(
							"All parameterTypes of sparql methods must be functional: "
									+ property.getURI());
				}
			}
			if (functional) {
				out.code(optimizer.implementQuery(field, params, range, null));
			} else {
				String set = Set.class.getName();
				out.code(optimizer.implementQuery(field, params, set, range));
			}
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

}
