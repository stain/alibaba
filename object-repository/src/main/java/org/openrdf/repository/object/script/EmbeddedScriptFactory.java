/*
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
package org.openrdf.repository.object.script;

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;

/**
 * Creates a CompiledScript from source code with a given context.
 *
 * @author James Leigh
 **/
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
		out.write("function ");
		out.write(getInvokeName());
		out.write("(msg) { ");
		out.write("try { ");
		out.write("return (function(msg) {");
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
		out.write("}).call(msg.msgTarget, msg);\n");
		out.write("} catch (e if e instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e);\n\t");
		out
				.write("} catch (e if e.javaException instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e.javaException);\n\t");
		out.write("}\n");
		out.write("}\n");
		return out.toString();
	}

	private CompiledScript createCompiledScript(ClassLoader cl,
			String systemId, String code) throws ObjectStoreConfigException,
			ScriptException {
		warnIfKeywordUsed(code);
		Thread current = Thread.currentThread();
		ClassLoader previously = current.getContextClassLoader();
		current.setContextClassLoader(cl);
		ScriptEngineManager man = new ScriptEngineManager();
		final ScriptEngine engine = man.getEngineByName("rhino");
		current.setContextClassLoader(previously);
		engine.put(ScriptEngine.FILENAME, systemId);
		engine.eval(code);
		return new CompiledScript() {
			public Object eval(ScriptContext context) throws ScriptException {
				Bindings bindings = context
						.getBindings(ScriptContext.ENGINE_SCOPE);
				Object msg = bindings.get("msg");
				String script = (String) bindings.get("script");
				try {
					return ((Invocable) engine).invokeFunction(getInvokeName(),
							msg);
				} catch (NoSuchMethodException e) {
					throw new BehaviourException(e, script);
				}
			}

			public ScriptEngine getEngine() {
				return engine;
			}
		};
	}

	private String getInvokeName() {
		return "_invoke" + Math.abs(hashCode());
	}
}
