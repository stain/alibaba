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
package org.openrdf.repository.object.managers.helpers;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openrdf.annotations.Bind;
import org.openrdf.model.Value;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.xslt.TransformBuilder;
import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * Compiles the XSLT code and manages worker threads to execute it.
 * 
 * @author James Leigh
 * 
 */
public class XSLTOptimizer {

	private static final Pattern IS_XML = Pattern.compile("^\\s*<");
	private static final Set<String> inputs;
	private static final Map<String, String> outputs;
	private static final Set<String> parameters;
	static {
		Set<String> set = new HashSet<String>();
		for (Method method : XSLTransformer.class.getMethods()) {
			if ("transform".equals(method.getName())
					&& method.getParameterTypes().length == 2) {
				set.add(method.getParameterTypes()[0].getName());
			}
		}
		inputs = Collections.unmodifiableSet(set);
		set = new HashSet<String>();
		for (Method method : TransformBuilder.class.getMethods()) {
			if ("with".equals(method.getName())
					&& method.getParameterTypes().length == 2) {
				set.add(method.getParameterTypes()[1].getName());
			}
		}
		parameters = Collections.unmodifiableSet(set);
		Map<String, String> map = new HashMap<String, String>();
		for (Method method : TransformBuilder.class.getMethods()) {
			if (method.getName().startsWith("as")
					&& method.getParameterTypes().length == 0) {
				map.put(method.getReturnType().getName(), method.getName());
			}
		}
		outputs = Collections.unmodifiableMap(map);
	}

	public boolean isKnownInputType(String javaClassName) {
		return inputs.contains(javaClassName);
	}

	public boolean isKnownOutputType(String javaClassName) {
		return outputs.containsKey(javaClassName);
	}

	public Class<?> getFieldType() {
		return XSLTransformer.class;
	}

	public String getFieldConstructor(String xslt, String base) {
		StringBuilder sb = new StringBuilder();
		sb.append("new ").append(XSLTransformer.class.getName());
		sb.append("(");
		if (IS_XML.matcher(xslt).find()) {
			sb.append("new ").append(StringReader.class.getName());
			sb.append("(").append(string(xslt)).append(")");
			sb.append(", ").append(string(base));
		} else {
			sb.append(string(xslt));
		}
		sb.append(")");
		return sb.toString();
	}

	public String implementXSLT(String field, Method method, List<String> args)
			throws ObjectStoreConfigException {
		Class<?> type = method.getReturnType();
		if (type.equals(Set.class))
			throw new ObjectStoreConfigException("XSLT return types must be functional");
		String output = type.getName();
		String input = null;
		String inputName = null;
		Class<?>[] ptypes = method.getParameterTypes();
		Map<String, String> parameters = new HashMap<String, String>(
				ptypes.length);
		Class<?> declaring = method.getDeclaringClass();
		parameters.put("this", parameterToCodedString(declaring, "this"));
		for (int i = 0; i < ptypes.length; i++) {
			boolean named = false;
			Class<?> ptype = ptypes[i];
			String code = parameterToCodedString(ptype, args.get(i));
			for (Annotation ann : method.getParameterAnnotations()[i]) {
				if (ann.annotationType().equals(Bind.class)) {
					for (String name : ((Bind) ann).value()) {
						named = true;
						parameters.put(name, code);
					}
				}
			}
			if (!named && input == null) {
				input = ptype.getName();
				inputName = args.get(i);
			} else if (!named) {
				throw new ObjectStoreConfigException("@"
						+ Bind.class.getSimpleName()
						+ " annotation not found: " + method.getName());
			}
		}
		return implementXSLT(field, input, inputName, parameters, output);
	}

	public String implementXSLT(String field, String input, String inputName,
			Map<String, String> parameters, String output)
			throws ObjectStoreConfigException {
		StringBuilder out = new StringBuilder();
		if (field == null) {
			out.append("new ");
			out.append(XSLTransformer.class.getName()).append("()");
		} else {
			out.append(field);
		}
		out.append(".transform(");
		if (input != null && inputs.contains(input)) {
			out.append(inputName);
			out.append(", getResource().stringValue()");
		} else if (input != null) {
			out.append("(").append(RDFObject.class.getName()).append(")");
			out.append(inputName);
			out.append(", getResource().stringValue()");
		}
		out.append(")");
		for (Map.Entry<String, String> e : parameters.entrySet()) {
			out.append(".with(").append(string(e.getKey()));
			out.append(", ").append(e.getValue()).append(")");
		}
		if (outputs.containsKey(output)) {
			out.append(".").append(outputs.get(output)).append("()");
		} else {
			throw new ObjectStoreConfigException("Unsupported XSLT return type: "
					+ output);
		}
		return out.toString();
	}

	private String parameterToCodedString(Class<?> type, String variable) {
		StringBuilder out = new StringBuilder();
		out.append(variable).append(" == null ? null : ");
		if (parameters.contains(type.getName())) {
			out.append(variable);
		} else if (type.isPrimitive()) {
			out.append("String.valueOf(");
			out.append(variable).append(")");
		} else if (Value.class.isAssignableFrom(type)) {
			out.append("((").append(Value.class.getName()).append(")");
			out.append(variable).append(").stringValue()");
		} else {
			out.append("getObjectConnection().getObjectFactory().createValue(");
			out.append(variable).append(").stringValue()");
		}
		return out.toString();
	}

	private String string(String str) {
		return "\""
				+ str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r",
						"\\r").replace("\n", "\\n") + "\"";
	}
}
