package org.openrdf.repository.object.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.util.ObjectResolver.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionScriptFactory implements ObjectFactory<CompiledScript> {
	private static final Pattern KEYWORDS = Pattern
			.compile("(?:var\\s+|\\.)(break|case|catch|const|continue|default|delete|do|else|export|finally|for|function|if|in|instanceof|import|name|new|return|switch|this|throw|try|typeof|var|void|while|with)\b");
	private final Logger logger = LoggerFactory
			.getLogger(FunctionScriptFactory.class);
	private final ClassLoader cl;

	public FunctionScriptFactory(ClassLoader cl) {
		this.cl = cl;
	}

	public CompiledScript create(String systemId, InputStream in)
			throws Exception {
		return create(systemId, new InputStreamReader(in, "UTF-8"));
	}

	public CompiledScript create(String systemId, Reader in) throws Exception {
		try {
			return createCompiledScript(cl, systemId, read(in));
		} finally {
			in.close();
		}
	}

	public String[] getContentTypes() {
		return new String[] { "text/javascript", "application/javascript",
				"text/ecmascript", "application/ecmascript" };
	}

	public boolean isReusable() {
		return true;
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int read;
		char[] cbuf = new char[1024];
		while ((read = in.read(cbuf)) >= 0) {
			out.write(cbuf, 0, read);
		}
		out.write("\n");
		out.write("function ");
		out.write(getInvokeName());
		out.write("(msg, funcname) {\n\t");
		out.write("return this[funcname].call(msg.msgTarget, msg);\n");
		out.write("}\n");
		return out.toString();
	}

	private String getInvokeName() {
		return "_invoke" + Math.abs(hashCode());
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
			engine.eval(code);
			return new CompiledScript() {
				public Object eval(ScriptContext context)
						throws ScriptException {
					Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
					Object msg = bindings.get("msg");
					String script = (String) bindings.get("script");
					String funcname = script.substring(script.indexOf('#') + 1);
					try {
						return ((Invocable) engine).invokeFunction(getInvokeName(), msg, funcname);
					} catch (NoSuchMethodException e) {
						throw new BehaviourException(e, script);
					}
				}

				public ScriptEngine getEngine() {
					return engine;
				}
			};
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

	protected void warnIfKeywordUsed(String code) {
		Matcher m = KEYWORDS.matcher(code);
		if (m.find()) {
			logger.warn("{} is a ECMA script keyword", m.group(1));
		}
	}
}