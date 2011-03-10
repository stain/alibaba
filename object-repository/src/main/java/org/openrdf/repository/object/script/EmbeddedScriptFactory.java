package org.openrdf.repository.object.script;

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedScriptFactory extends FunctionScriptFactory {
	private static final String BEHAVIOUR = BehaviourException.class.getName();
	private static final Map<Class<?>, String> primitives = new HashMap<Class<?>, String>();
	static {
		primitives.put(Boolean.TYPE, ".booleanValue()");
		primitives.put(Character.TYPE, ".charValue()");
		primitives.put(Byte.TYPE, ".byteValue()");
		primitives.put(Short.TYPE, ".shortValue()");
		primitives.put(Integer.TYPE, ".intValue()");
		primitives.put(Long.TYPE, ".longValue()");
		primitives.put(Float.TYPE, ".floatValue()");
		primitives.put(Double.TYPE, ".doubleValue()");
	}
	private final Logger logger = LoggerFactory
			.getLogger(EmbeddedScriptFactory.class);
	private final ClassLoader cl;
	private EmbeddedScriptContext context;

	public EmbeddedScriptFactory(ClassLoader cl, EmbeddedScriptContext context) {
		super(cl);
		this.cl = cl;
		this.context = context;
	}

	public CompiledScript create(String systemId, Reader in) throws Exception {
		try {
			return createCompiledScript(cl, systemId, read(in));
		} finally {
			in.close();
		}
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		for (String pkg : context.getPackages()) {
			warnIfKeywordUsed(pkg);
			out.write("importPackage(Packages.");
			out.write(pkg);
			out.write("); ");
		}
		for (String className : context.getClasses()) {
			warnIfKeywordUsed(className);
			out.write("importClass(Packages.");
			out.write(className);
			out.write("); ");
		}
		out.write("(function(msg) { try{ ");
		for (Map.Entry<String, String> e : context.getAssignments().entrySet()) {
			warnIfKeywordUsed(e.getKey());
			out.write("var ");
			out.write(e.getKey());
			out.write(" = msg.getMsgTarget().");
			out.write(GET_CONNECTION);
			out.write("().getObject(\"");
			out.write(e.getValue());
			out.write("\"); ");
		}
		if (context.isWithThis()) {
			out.write("with(this) { ");
		}
		out.write("with(msg) { ");
		int read;
		char[] cbuf = new char[1024];
		while ((read = in.read(cbuf)) >= 0) {
			out.write(cbuf, 0, read);
		}
		out.write("\n\t\t");
		out.write("}\n\t");
		if (context.isWithThis()) {
			out.write("}\n\t");
		}
		out.write("} catch (e if e instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e);\n\t");
		out
				.write("} catch (e if e.javaException instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e.javaException);\n\t");
		out.write("}\n");
		out.write("}).call(msg.msgTarget, msg);\n");
		return out.toString();
	}

	private CompiledScript createCompiledScript(ClassLoader cl,
			String systemId, String code) throws ObjectStoreConfigException {
		warnIfKeywordUsed(code);
		Thread current = Thread.currentThread();
		ClassLoader previously = current.getContextClassLoader();
		current.setContextClassLoader(cl);
		ScriptEngineManager man = new ScriptEngineManager();
		final ScriptEngine engine = man.getEngineByName("ECMAScript");
		current.setContextClassLoader(previously);
		try {
			engine.put(ScriptEngine.FILENAME, systemId);
			return ((Compilable) engine).compile(code);
		} catch (final ScriptException exc) {
			logger.error(exc.getMessage());
			return new CompiledScript() {
				public Object eval(javax.script.ScriptContext context)
						throws ScriptException {
					throw new ScriptException(exc);
				}

				public ScriptEngine getEngine() {
					return engine;
				}
			};
		}
	}
}