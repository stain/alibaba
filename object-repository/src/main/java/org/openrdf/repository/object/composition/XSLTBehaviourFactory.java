/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.annotations.iri;
import org.openrdf.model.Resource;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.helpers.XSLTOptimizer;
import org.openrdf.repository.object.vocabulary.MSG;

/**
 * Generate a behaviour for msg:xslt annotated methods.
 * 
 * @author James Leigh
 * 
 */
public class XSLTBehaviourFactory extends BehaviourFactory {

	@Override
	protected boolean isEnhanceable(Class<?> concept) throws Exception {
		for (Method m : concept.getDeclaredMethods()) {
			if (getXslValue(m) != null)
				return true;
		}
		return false;
	}

	@Override
	protected void enhance(ClassTemplate cc, Class<?> concept) throws Exception {
		addRDFObjectMethod(cc);
		int count = 0;
		for (Method m : concept.getDeclaredMethods()) {
			if (getXslValue(m) != null) {
				enhance(cc, m, ++count);
			}
		}
	}

	private String getXslValue(Method m) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		for (Annotation ann : m.getAnnotations()) {
			String iri = getIriValue(ann.annotationType());
			if (MSG.XSLT.stringValue().equals(iri)) {
				return getValue(ann);
			}
		}
		return null;
	}

	private String getValue(Annotation ann) {
		try {
			Method value = ann.annotationType().getMethod("value");
			Object ret = value.invoke(ann);
			if (ret instanceof String)
				return (String) ret;
			if (ret instanceof Object[]) {
				for (Object o : (Object[]) ret) {
					if (o instanceof String) {
						return (String) o;
					}
				}
			}
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	private String getIriValue(Class<?> type) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		iri iri = type.getAnnotation(iri.class);
		if (iri == null)
			return null;
		return iri.value();
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

	private void enhance(ClassTemplate cc, Method m, int count)
			throws Exception {
		String xslt = getXslValue(m);
		String base;
		if (m.getDeclaringClass().isAnnotationPresent(iri.class)) {
			base = m.getDeclaringClass().getAnnotation(iri.class).value();
		} else {
			base = "java:" + m.getDeclaringClass().getName();
		}
		XSLTOptimizer optimizer = new XSLTOptimizer();
		String field = m.getName() + "XSLT" + count;
		CodeBuilder out = cc.assignStaticField(optimizer.getFieldType(), field);
		out.code(optimizer.getFieldConstructor(xslt, base)).end();
		int argc = m.getParameterTypes().length;
		List<String> args = new ArrayList<String>(argc);
		for (int i = 1; i <= argc; i++) {
			args.add("$" + i);
		}
		out = cc.overrideMethod(m, m.isBridge());
		out.code("try {\n");
		String str = optimizer.implementXSLT(field, m, args);
		out.code("return ").code(str).semi();
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

}
