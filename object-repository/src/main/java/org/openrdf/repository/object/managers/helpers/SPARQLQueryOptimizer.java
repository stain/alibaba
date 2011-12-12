/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.repository.object.managers.helpers.SparqlEvaluator.SparqlBuilder;

/**
 * Rewrites the SPARQL query used by sparql behaviour methods by loading
 * additional properties.
 * 
 * @author James Leigh
 * 
 */
public class SPARQLQueryOptimizer {
	private static final Pattern IS_URL = Pattern.compile("^\\w+:[^<>\\s{}]+$");
	private Pattern startsWithPrefix = Pattern.compile("\\s*PREFIX\\s.*",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Map<String, String> outputs;
	private static final Map<String, String> parameterized;
	static {
		Map<String, String> map = new HashMap<String, String>();
		for (Method method : SparqlBuilder.class.getMethods()) {
			if (method.getName().startsWith("as")
					&& method.getParameterTypes().length == 0) {
				map.put(method.getReturnType().getName(), method.getName());
			}
		}
		outputs = Collections.unmodifiableMap(map);
		map = new HashMap<String, String>();
		for (Method method : SparqlBuilder.class.getMethods()) {
			if (method.getName().startsWith("as")
					&& method.getParameterTypes().length == 1
					&& method.getParameterTypes()[0].equals(Class.class)) {
				map.put(method.getReturnType().getName(), method.getName());
			}
		}
		parameterized = Collections.unmodifiableMap(map);
	}

	public Class<?> getFieldType() {
		return SparqlEvaluator.class;
	}

	public String getFieldConstructor(String query, String base,
			Map<String, String> namespaces) {
		if (!IS_URL.matcher(query).matches()) {
			query = prefixQueryString(query, namespaces);
		}
		return getFieldConstructor(query, base, (PropertyMapper) null);
	}

	public String getFieldConstructor(String query, String base,
			PropertyMapper pm) {
		boolean readingTypes = pm == null || pm.isReadingTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("new ").append(SparqlEvaluator.class.getName());
		sb.append("(");
		if (IS_URL.matcher(query).find()) {
			sb.append(string(query));
		} else {
			sb.append("new ").append(StringReader.class.getName());
			sb.append("(").append(string(query)).append(")");
			sb.append(", ").append(string(base));
		}
		sb.append(", ").append(readingTypes).append(")");
		return sb.toString();
	}

	public String implementQuery(String field, Map<String, String> parameters,
			String returnType, String componentType)
			throws ObjectStoreConfigException {
		StringBuilder out = new StringBuilder();
		boolean component = componentType != null
				&& !Object.class.getName().equals(componentType);
		if (!component && "void".equals(returnType)) {
			out.append(field);
			out.append(".prepare(getObjectConnection())");
			out.append(".with(\"this\", getResource())");
			for (Map.Entry<String, String> param : parameters.entrySet()) {
				out.append(".with(").append(string(param.getKey()));
				out.append(", ").append(param.getValue()).append(")");
			}
			out.append(".asUpdate();");
		} else if (!component && outputs.containsKey(returnType)) {
			out.append("return ").append(field);
			out.append(".prepare(getObjectConnection())");
			out.append(".with(\"this\", getResource())");
			for (Map.Entry<String, String> param : parameters.entrySet()) {
				out.append(".with(").append(string(param.getKey()));
				out.append(", ").append(param.getValue()).append(")");
			}
			out.append(".").append(outputs.get(returnType)).append("();");
		} else {
			out.append("return (").append(returnType).append(") ");
			out.append(field);
			out.append(".prepare(getObjectConnection())");
			out.append(".with(\"this\", getResource())");
			for (Map.Entry<String, String> param : parameters.entrySet()) {
				out.append(".with(").append(string(param.getKey()));
				out.append(", ").append(param.getValue()).append(")");
			}
			if (componentType != null && parameterized.containsKey(returnType)) {
				out.append(".").append(outputs.get(returnType));
				out.append("(java.lang.Class.forName(");
				out.append(string(componentType));
				out.append(", true, getClass().getClassLoader()));");
			} else {
				out.append(".as(java.lang.Class.forName(");
				out.append(string(returnType));
				out.append(", true, getClass().getClassLoader()));");
			}
		}
		return out.toString();
	}

	private String string(String str) {
		if (str == null)
			return "null";
		return "\""
				+ str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r",
						"\\r").replace("\n", "\\n") + "\"";
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
}
