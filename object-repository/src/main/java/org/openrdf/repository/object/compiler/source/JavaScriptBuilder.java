package org.openrdf.repository.object.compiler.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.script.EmbededScriptEngine;
import org.openrdf.repository.object.script.EmbededScriptEngine.ScriptResult;
import org.openrdf.repository.object.vocabulary.MSG;
import org.openrdf.repository.object.vocabulary.OBJ;

public class JavaScriptBuilder extends JavaMessageBuilder {
	private static final String JAVA_NS = "java:";
	private static final Pattern IS_URL = Pattern.compile("^\\w+:[^<>\\s{}]+$");
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");
	private static final Map<String, String> outputs;
	static {
		Map<String, String> map = new HashMap<String, String>();
		for (Method method : ScriptResult.class.getMethods()) {
			if (method.getName().startsWith("as")
					&& method.getParameterTypes().length == 0) {
				map.put(method.getReturnType().getName(), method.getName());
			}
		}
		outputs = Collections.unmodifiableMap(map);
	}

	public JavaScriptBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
	}

	public void engine(String simple, RDFClass method, String code,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		String field = "scriptEngine";
		String fileName = method.getURI().stringValue();
		staticField(imports(EmbededScriptEngine.class), field, "null");
		code("\tstatic {\n\t\t");
		code("java.lang.ClassLoader cl = ").code(simple).code(
				".class.getClassLoader();\n\t\t");
		if (IS_URL.matcher(code).matches()) {
			code(field).code(" = ");
			code(imports(EmbededScriptEngine.class));
			code(".newInstance(cl, ").code(quote(code)).code(");\n\t\t");
		} else {
			code(field).code(" = ");
			code(imports(EmbededScriptEngine.class));
			code(".newInstance(cl, ").code(quote(code)).code(", ");
			code(quote(fileName)).code(");\n\t\t");
		}
		Set<RDFClass> imports = method.getRDFClasses(MSG.IMPORTS);
		imports.addAll(method.getRDFClasses(OBJ.IMPORTS));
		for (RDFEntity imp : imports) {
			URI uri = imp.getURI();
			boolean isJava = uri.getNamespace().equals(JAVA_NS);
			if (isJava || imp.isA(OWL.CLASS)) {
				String className = getClassName(uri);
				if (isJava && !isJavaClassName(className)) {
					code(field).code(".importPackage(");
					code(quote(className)).code(");\n\t\t");
				} else {
					code(field).code(".importClass(");
					code(quote(className)).code(");\n\t\t");
				}
			} else if (uri != null) {
				String name = var(resolver.getSimpleName(uri));
				code(field).code(".assignRDFObject(").code(quote(name));
				code(", ").code(quote(uri.stringValue())).code(");\n\t\t");
			}
		}
		if (method.getString(OBJ.SCRIPT) != null) {
			code(field).code(".withThis();\n\t\t");
		}
		RDFProperty response = method.getResponseProperty();
		boolean isVoid = NOTHING.equals(method.getRange(response).getURI());
		String objectRange = getRangeObjectClassName(method, response);
		String range = getRangeClassName(method, response);
		boolean functional = method.isFunctional(response);
		boolean isPrimitive = !objectRange.equals(range)
				&& functional;
		code(field).code(".returnType(");
		if (isVoid) {
			code(Void.class.getName()).code(".TYPE");
		} else if (functional) {
			code(Set.class.getName()).code(".class");
		} else if (isPrimitive) {
			code(objectRange).code(".TYPE");
		} else if (outputs.containsKey(objectRange)) { // Number
			code(objectRange).code(".class");
		} else {
			code(Object.class.getName()).code(".class");
		}
		code(");\n\t\t");
		code("}\n");
	}

	public JavaScriptBuilder script(RDFClass msg, RDFClass method, String code,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		String field = "scriptEngine";
		RDFProperty response = msg.getResponseProperty();
		boolean isVoid = NOTHING.equals(method.getRange(response).getURI());
		String range = getRangeClassName(msg, response);
		StringBuilder out = new StringBuilder();
		if (!msg.isFunctional(response)) {
			out.append("return ");
			out.append(field).append(".call(msg)");
			out.append(".asSet();");
		} else if (isVoid) {
			out.append(field).append(".call(msg).asVoid();");
		} else if (outputs.containsKey(range)) { // Number or primitive
			out.append("return ");
			out.append(field).append(".call(msg)");
			out.append(".").append(outputs.get(range)).append("();");
		} else {
			out.append("return (").append(range).append(") ");
			out.append(field).append(".call(msg)");
			out.append(".asObject();");
		}
		message(msg, method, false, out.toString());
		return this;
	}

	private boolean isJavaClassName(String className) {
		return resolver.isJavaClass(className);
	}

	private String quote(String string) {
		return "\""
				+ string.replace("\\", "\\\\").replace("\"", "\\\"").replace(
						"\n", "\\n") + "\"";
	}

}
