/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.object.ObjectQuery;
import org.openrdf.repository.object.annotations.name;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.managers.PropertyMapper;
import org.openrdf.result.MultipleResultException;
import org.openrdf.result.Result;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * Rewrites the SPARQL query used by sparql behaviour methods by loading
 * additional properties.
 * 
 * @author James Leigh
 * 
 */
public class SPARQLQueryOptimizer {

	private static final Pattern selectWhere = Pattern.compile("\\sSELECT\\s+([\\?\\$]\\w+)\\s+WHERE\\s*\\{", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public String implementQuery(String sparql, String base, Method method,
			List<String> args, PropertyMapper pm) throws ObjectStoreConfigException {
		Class<?> type = method.getReturnType();
		String range = type.getName();
		boolean primitive = type.isPrimitive();
		boolean functional = !type.equals(Set.class);
		Map<String, String> eager = null;
		if (functional && !primitive) {
			eager = pm.findEagerProperties(type);
		} else if (!primitive) {
			range = Object.class.getName();
			Type t = method.getGenericReturnType();
			if (t instanceof ParameterizedType) {
				Type c = ((ParameterizedType) t).getActualTypeArguments()[0];
				if (c instanceof Class) {
					range = ((Class<?>) c).getName();
					eager = pm.findEagerProperties((Class<?>) c);
				}
			}
		}
		Class<?>[] ptypes = method.getParameterTypes();
		Map<String, String> parameters = new HashMap<String, String>(
				ptypes.length);
		for (int i = 0; i < ptypes.length; i++) {
			boolean named = false;
			for (Annotation ann : method.getParameterAnnotations()[i]) {
				if (ann.annotationType().equals(name.class)) {
					for (String name : ((name) ann).value()) {
						named = true;
						String arg = args.get(i);
						if (ptypes[i].isPrimitive()) {
							parameters.put(name, getBindingPrimitive(arg));
						} else if (Value.class.isAssignableFrom(ptypes[i])) {
							parameters.put(name, getValue(arg));
						} else {
							parameters.put(name, getBindingValue(arg));
						}
					}
				}
			}
			if (!named)
				throw new ObjectStoreConfigException("@name annotation not found: " + method.getName());
		}
		return implementQuery(sparql, base, eager, wrap(range), range,
				functional, parameters);
	}

	public String implementQuery(String qry, String base,
			Map<String, String> eager, String range, String primitiveRange,
			boolean functional, Map<String, String> parameters)
			throws ObjectStoreConfigException {
		StringBuilder out = new StringBuilder();
		boolean objectQuery = prepareQuery(qry, base, range, eager, out);
		for (Map.Entry<String, String> e : parameters.entrySet()) {
			out.append("qry.setBinding(").append(string(e.getKey())).append(", ");
			out.append(e.getValue());
			out.append(");\n\t\t\t");
		}
		if (objectQuery) {
			evaluateObjectQuery(qry, out, range, primitiveRange, functional);
		} else if (Model.class.getName().equals(range)) {
			evaluateModelQuery(out, functional);
		} else if (functional && Statement.class.getName().equals(range)) {
			evaluateStatementQuery(out);
		} else if (Statement.class.getName().equals(range)) {
			evaluateModelQuery(out, functional);
		} else if (BindingSet.class.getName().equals(range)) {
			evaluateBindingSetQuery(out, functional);
		} else {
			evaluateQuery(out, functional);
		}
		return out.toString();
	}

	private String getBindingValue(String arg) {
		StringBuilder out = new StringBuilder();
		out.append("getObjectConnection().getObjectFactory().createValue(");
		out.append(arg).append(")");
		return out.toString();
	}

	private String getValue(String arg) {
		StringBuilder out = new StringBuilder();
		out.append("((").append(Value.class.getName()).append(")");
		out.append(arg).append(")");
		return out.toString();
	}

	private String getBindingPrimitive(String arg) {
		StringBuilder out = new StringBuilder();
		out.append("getObjectConnection().getValueFactory().createLiteral(");
		out.append(arg).append(")");
		return out.toString();
	}

	private String string(String str) {
		return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
	}

	private boolean prepareQuery(String qry, String base, String range,
			Map<String, String> eager, StringBuilder out)
			throws ObjectStoreConfigException {
		boolean booleanQuery = isBooleanQuery(qry, base);
		boolean objectQuery = false;
		if (booleanQuery) {
			out.append(BooleanQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareBooleanQuery(");
		} else if (GraphQueryResult.class.getName().equals(range)) {
			out.append(GraphQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareGraphQuery(");
		} else if (Model.class.getName().equals(range)) {
			out.append(GraphQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareGraphQuery(");
		} else if (Statement.class.getName().equals(range)) {
			out.append(GraphQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareGraphQuery(");
		} else if (BindingSet.class.getName().equals(range)) {
			out.append(TupleQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareTupleQuery(");
		} else if (TupleQueryResult.class.getName().equals(range)) {
			out.append(TupleQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareTupleQuery(");
		} else {
			objectQuery = true;
			out.append(ObjectQuery.class.getName()).append(" qry;\n\t\t\t");
			out.append("qry = getObjectConnection().prepareObjectQuery(");
		}
		out.append(QueryLanguage.class.getName()).append(".SPARQL, \n\t\t\t");
		if (objectQuery) {
			out.append(string(optimizeQueryString(qry, eager)));
		} else {
			out.append(string(qry));
		}
		out.append(", ").append(string(base));
		out.append(");\n\t\t\t");
		out.append("qry.setBinding(\"this\", getResource());\n\t\t\t");
		return objectQuery;
	}

	/** @param map property name to predicate uri or null for datatype */
	private String optimizeQueryString(String sparql, Map<String, String> map) {
		Matcher matcher = selectWhere.matcher(sparql);
		if (map != null && matcher.find()) {
			String var = matcher.group(1);
			int idx = sparql.lastIndexOf('}');
			StringBuilder sb = new StringBuilder(256 + sparql.length());
			sb.append(sparql, 0, matcher.start(1));
			sb.append(var).append(" ");
			sb.append(var).append("_class").append(" ");
			for (Map.Entry<String, String> e : map.entrySet()) {
				String name = e.getKey();
				if (name.equals("class"))
					continue;
				sb.append(var).append("_").append(name).append(" ");
			}
			sb.append(sparql, matcher.end(1), idx);
			sb.append(" OPTIONAL { ").append(var);
			sb.append(" a ").append(var).append("_class}");
			for (Map.Entry<String, String> e : map.entrySet()) {
				String pred = e.getValue();
				String name = e.getKey();
				if (name.equals("class"))
					continue;
				sb.append(" OPTIONAL { ").append(var).append(" <");
				sb.append(pred).append("> ");
				sb.append(var).append("_").append(name).append("}");
			}
			sb.append(sparql, idx, sparql.length());
			sparql = sb.toString();
		}
		return sparql;
	}

	private void evaluateObjectQuery(String sparql, StringBuilder out,
			String range, String primitiveRange, boolean functional) {
		if (selectWhere.matcher(sparql).find()) {
			out.append(Result.class.getName()).append(" result;\n\t\t\t");
			out.append("result = qry.evaluate(");
			out.append(range).append(".class");
			out.append(");\n\t\t\t");
		} else {
			out.append(Result.class.getName()).append(" result;\n\t\t\t");
			out.append("result = qry.evaluate();\n\t\t\t");
		}
		if (functional && !range.equals(primitiveRange)) {
			out.append("return ((").append(range).append(")result.singleResult()).");
			out.append(primitiveRange).append("Value();");
		} else if (Result.class.getName().equals(range)) {
			out.append("return result;");
		} else if (functional) {
			out.append("try {\n\t\t\t");
			out.append(range).append(" next = (").append(range).append(")result.next();\n\t\t\t");
			out.append("if (result.next() != null)\n\t\t\t");
			out.append("throw new ").append(MultipleResultException.class.getName());
			out.append("(").append(string("More than one result")).append(");\n\t\t\t");
			out.append("return next;\n\t\t\t");
			out.append("} finally {\n\t\t\t");
			out.append("result.close();\n\t\t\t");
			out.append("}");
		} else {
			out.append("return result.asSet();");
		}
	}

	private void evaluateBindingSetQuery(StringBuilder out, boolean functional) {
		out.append(TupleQueryResult.class.getName());
		out.append(" result = ").append("qry.evaluate();");
		out.append("try {");
		if (functional) {
			out.append("if (result.hasNext())");
			out.append("return result.next();");
			out.append("return null;");
		} else {
			out.append("if (result.hasNext())");
			out.append("return ");
			out.append(Collections.class.getName());
			out.append(".singleton(model);");
			out.append("return ");
			out.append(Collections.class.getName());
			out.append(".emptySet();");
		}
		out.append("} finally {");
		out.append("result.close();");
		out.append("}");
	}

	private void evaluateStatementQuery(StringBuilder out) {
		out.append(GraphQueryResult.class.getName());
		out.append(" result = ").append("qry.evaluate();");
		out.append("try {");
		out.append("if (result.hasNext())");
		out.append("return result.next();");
		out.append("return null;");
		out.append("} finally {");
		out.append("result.close();");
		out.append("}");
	}

	private void evaluateModelQuery(StringBuilder out, boolean functional) {
		out.append(Model.class.getName()).append(" model = new ");
		out.append(LinkedHashModel.class.getName()).append("();");
		out.append("qry.evaluate(new ").append(StatementCollector.class.getName());
		out.append("(model));");
		if (functional) {
			out.append("return model;");
		} else {
			out.append("return ");
			out.append(Collections.class.getName());
			out.append(".singleton(model);");
		}
	}

	private void evaluateQuery(StringBuilder out, boolean functional) {
		if (functional) {
			out.append("return qry.evaluate();");
		} else {
			out.append("return ");
			out.append(Collections.class.getName());
			out.append(".singleton(qry.evaluate());");
		}
	}

	private boolean isBooleanQuery(String qry, String base)
			throws ObjectStoreConfigException {
		SPARQLParser parser = new SPARQLParser();
		try {
			ParsedQuery query = parser.parseQuery(qry, base);
			return query instanceof ParsedBooleanQuery;
		} catch (MalformedQueryException e) {
			throw new ObjectStoreConfigException(e);
		}
	}

	public String wrap(String type) {
		if (type.equals("char"))
			return "java.lang.Character";
		else if (type.equals("byte"))
			return "java.lang.Byte";
		else if (type.equals("short"))
			return "java.lang.Short";
		else if (type.equals("int"))
			return "java.lang.Integer";
		else if (type.equals("long"))
			return "java.lang.Long";
		else if (type.equals("float"))
			return "java.lang.Float";
		else if (type.equals("double"))
			return "java.lang.Double";
		else if (type.equals("boolean"))
			return "java.lang.Boolean";
		else if (type.equals("void"))
			return "java.lang.Void";
		return type;
	}
}
