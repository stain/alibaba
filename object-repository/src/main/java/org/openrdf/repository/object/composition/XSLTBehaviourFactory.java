/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.xslt;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.helpers.XSLTOptimizer;

/**
 * Generate a behaviour for {@link xslt} annotated methods.
 * 
 * @author James Leigh
 * 
 */
public class XSLTBehaviourFactory extends BehaviourFactory {

	private static final String CLASS_PREFIX = "object.behaviours.";

	@Override
	protected String getJavaClassName(Class<?> concept) {
		String cn = concept.getName();
		return CLASS_PREFIX + cn + "XSLT";
	}

	@Override
	protected boolean isEnhanceable(Class<?> concept)
			throws ObjectStoreConfigException {
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(xslt.class))
				return true;
		}
		return false;
	}

	@Override
	protected void enhance(ClassTemplate cc, Class<?> concept) throws Exception {
		int count = 0;
		for (Method m : concept.getDeclaredMethods()) {
			if (m.isAnnotationPresent(xslt.class)) {
				enhance(cc, m, ++count);
			}
		}
	}

	private void enhance(ClassTemplate cc, Method m, int count)
			throws Exception {
		String xslt = m.getAnnotation(xslt.class).value();
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
		out.code(str);
		out.code("\n} catch(");
		out.code(RuntimeException.class.getName()).code(" e) {");
		out.code("throw e;");
		out.code("\n} catch(");
		out.code(Exception.class.getName()).code(" e) {");
		out.code("throw new ");
		out.code(BehaviourException.class.getName()).code("(e);");
		out.code("}");
		out.end();
	}

}
