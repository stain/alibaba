/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.repository.object.composition;

import static org.openrdf.repository.object.traits.RDFObjectBehaviour.GET_ENTITY_METHOD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Resource;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.SPARQLQueryOptimizer;
import org.openrdf.repository.object.util.GenericType;

/**
 * Generate a behaviour for {@link Sparql} annotated methods.
 * 
 * @author James Leigh
 * 
 */
public class SparqlBehaviourFactory extends BehaviourFactory {

	@Override
	protected boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException {
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(Sparql.class))
				return true;
		}
		return false;
	}

	@Override
	protected void enhance(ClassTemplate cc, Class<?> concept) throws Exception {
		addRDFObjectMethod(cc);
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(Sparql.class)) {
				enhance(cc, m);
			}
		}
	}

	private void addRDFObjectMethod(ClassTemplate cc)
			throws ObjectCompositionException, NoSuchMethodException {
		MethodBuilder m = cc.createPrivateMethod(ObjectConnection.class,
				RDFObject.GET_CONNECTION);
		m.code("return (").cast(RDFObject.class).code(GET_ENTITY_METHOD);
		m.code("()).").code(RDFObject.GET_CONNECTION).code("();").end();
		m = cc.createPrivateMethod(Resource.class, RDFObject.GET_RESOURCE);
		m.code("return (").cast(RDFObject.class).code(GET_ENTITY_METHOD);
		m.code("()).").code(RDFObject.GET_RESOURCE).code("();").end();
	}

	private void enhance(ClassTemplate cc, Method m) throws Exception {
		String sparql = m.getAnnotation(Sparql.class).value();
		String base;
		if (m.getDeclaringClass().isAnnotationPresent(Iri.class)) {
			base = m.getDeclaringClass().getAnnotation(Iri.class).value();
		} else {
			base = "java:" + m.getDeclaringClass().getName();
		}
		int argc = m.getParameterTypes().length;
		List<String> args = new ArrayList<String>(argc);
		for (int i = 1; i <= argc; i++) {
			args.add("$" + i);
		}
		SPARQLQueryOptimizer oqo = new SPARQLQueryOptimizer();
		String fieldName = "_$sparql" + toHexString(m);
		String field = oqo.getFieldConstructor(sparql, base, properties);
		cc.assignStaticField(oqo.getFieldType(), fieldName).code(field).end();
		CodeBuilder out = cc.overrideMethod(m, m.isBridge());
		out.code("try {\n");
		out.code(implementQuery(oqo, fieldName, m, args));
		out.code("\n} catch(");
		out.code(RuntimeException.class.getName()).code(" e) {");
		out.code("throw e;");
		out.code("\n} catch(");
		out.code(Exception.class.getName()).code(" e) {");
		out.code("throw new ");
		out.code(BehaviourException.class.getName()).code("(e, ");
		out.insert(base).code(");");
		out.code("}");
		out.end();
	}

	private String toHexString(Method m) {
		List<Class<?>> p = Arrays.asList(m.getParameterTypes());
		return Integer.toHexString(Math.abs(31 * m.hashCode() + p.hashCode()));
	}

	private String implementQuery(SPARQLQueryOptimizer oqo, String field, Method method, List<String> args)
			throws ObjectStoreConfigException {
		GenericType type = new GenericType(method.getGenericReturnType());
		Class<?>[] ptypes = method.getParameterTypes();
		Map<String, String> parameters = new HashMap<String, String>(
				ptypes.length);
		loop: for (int i = 0; i < ptypes.length; i++) {
			for (Annotation ann : method.getParameterAnnotations()[i]) {
				if (ann.annotationType().equals(Bind.class)) {
					for (String name : ((Bind) ann).value()) {
						String arg = args.get(i);
						parameters.put(name, arg);
						continue loop;
					}
				}
			}
			throw new ObjectStoreConfigException("@"
					+ Bind.class.getSimpleName() + " annotation not found: "
					+ method.getName());
		}
		String primary = type.getClassType().getName();
		Class<?> ctype = type.getComponentClass();
		String component = ctype == null ? null : ctype.getName();
		return oqo.implementQuery(field, parameters, primary, component);
	}

}
