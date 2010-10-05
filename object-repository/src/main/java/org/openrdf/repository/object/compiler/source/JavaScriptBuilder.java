package org.openrdf.repository.object.compiler.source;

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.object.compiler.JavaNameResolver;
import org.openrdf.repository.object.compiler.model.RDFClass;
import org.openrdf.repository.object.compiler.model.RDFEntity;
import org.openrdf.repository.object.compiler.model.RDFProperty;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.vocabulary.OBJ;

public class JavaScriptBuilder extends JavaMessageBuilder {
	private static final String JAVA_NS = "java:";
	private static final URI NOTHING = new URIImpl(OWL.NAMESPACE + "Nothing");

	public JavaScriptBuilder(File source, JavaNameResolver resolver)
			throws FileNotFoundException {
		super(source, resolver);
	}

	public void engine(RDFClass method, String code,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		String field = "scriptEngine";
		String newScriptEngine = "new " + imports(ScriptEngineManager.class)
				+ "().getEngineByName(\"ECMAScript\")";
		String methodName = resolver.getMethodName(method.getURI());
		StringBuilder script = new StringBuilder();
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			URI uri = imp.getURI();
			String name = uri.getLocalName();
			boolean isJava = uri.getNamespace().equals(JAVA_NS);
			if (isJava && name.endsWith(".")) {
				String className = name.substring(0, name.length() - 1);
				script.append("importPackage(Packages.").append(className).append(");");
			} else if (isJava) {
				String className = getClassName(uri);
				script.append("importClass(Packages.").append(className).append(");");
			} else if (imp.isA(OWL.CLASS)) {
				String className = getClassName(uri);
				script.append("importClass(Packages.").append(className).append(");");
			}
		}
		script.append("function ").append(methodName).append("(msg) {");
		importVariables(script, method);
		script.append("with(this) { with(msg) {");
		script.append(code).append("\n} } }\n");
		script.append("function invokeFunction(funcName, msg) {\n\t");
		script.append("return this[funcName].call(msg.target, msg);\n}\n");
		String fileName = method.getURI().stringValue();
		try {
			ScriptEngine eng = new ScriptEngineManager().getEngineByName("ECMAScript");
			eng.put(ScriptEngine.FILENAME, fileName);
			eng.eval(script.toString());
		} catch (ScriptException cause) {
			throw new ObjectStoreConfigException(cause);
		}
		staticField(imports(ScriptEngine.class), field, newScriptEngine);
		code("\tstatic {\n\t\ttry {\n\t\t\t").code(field).code(".put(");
		code(quote(ScriptEngine.FILENAME)).code(", ");
		code(quote(fileName)).code(");\n\t\t\t");
		code(field).code(".eval(");
		code(quote(script.toString())).code(");\n\t\t} catch (");
		code(imports(ScriptException.class)).code(" exc) {}\n\t}\n");
	}

	public JavaScriptBuilder script(RDFClass msg, RDFClass method, String code,
			Map<String, String> namespaces) throws ObjectStoreConfigException {
		String field = "scriptEngine";
		String methodName = resolver.getMethodName(method.getURI());
		StringBuilder out = new StringBuilder();
		RDFProperty response = msg.getResponseProperty();
		boolean isVoid = NOTHING.equals(method.getRange(response).getURI());
		String objectRange = getRangeObjectClassName(msg, response);
		String range = getRangeClassName(msg, response);
		boolean isPrimitive = msg.isFunctional(response) && !objectRange.equals(range);
		if (!isVoid) {
			out.append("return ");
			if (isPrimitive) {
				out.append("((").append(imports(objectRange)).append(") ");
			} else if (msg.isFunctional(response)) {
				out.append("(").append(imports(range)).append(") ");
			} else {
				out.append("(").append(imports(Set.class)).append(") ");
			}
		}
		out.append("((").append(imports(Invocable.class)).append(")");
		out.append(field).append(").invokeFunction(\"invokeFunction\", ");
		out.append(quote(methodName)).append(", msg)");
		if (isPrimitive) {
			out.append(").").append(range).append("Value()");
		}
		out.append(";");
		message(msg, method, false, out.toString());
		return this;
	}

	private String quote(String string) {
		return "\""
				+ string.replace("\\", "\\\\").replace("\"", "\\\"").replace(
						"\n", "\\n") + "\"";
	}

	private void importVariables(StringBuilder out, RDFEntity method)
			throws ObjectStoreConfigException {
		Set<? extends RDFEntity> imports = method.getRDFClasses(OBJ.IMPORTS);
		for (RDFEntity imp : imports) {
			URI subj = imp.getURI();
			if (!imp.getURI().getNamespace().equals(JAVA_NS)
					&& !imp.isA(OWL.CLASS) && subj != null) {
				String name = var(resolver.getSimpleName(subj));
				out.append("var ").append(name);
				out.append(" = msg.getTarget().");
				out.append(GET_CONNECTION).append("().getObject(\"");
				out.append(subj.stringValue()).append("\"); ");
			}
		}
	}

}
