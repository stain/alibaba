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

import java.io.StringReader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.util.ObjectResolver;

/**
 * Facade to execute EMCAScript code from URL or code block.
 *
 * @author James Leigh
 **/
public class EmbededScriptEngine {
	private static final Map<ClassLoader, Map<String, Reference<ObjectResolver<CompiledScript>>>> scripts = new WeakHashMap<ClassLoader, Map<String, Reference<ObjectResolver<CompiledScript>>>>();
	static {
		// load the script engine now, to import any binary libraries
		if (null == new ScriptEngineManager().getEngineByName("ECMAScript"))
			throw new AssertionError("ECMAScript not available");
	}

	public static synchronized EmbededScriptEngine newInstance(
			final ClassLoader cl, String systemId) {
		if (systemId.contains("#")) {
			String url = systemId.substring(0, systemId.indexOf('#'));
			Map<String, Reference<ObjectResolver<CompiledScript>>> map = scripts
					.get(cl);
			if (map == null) {
				scripts.put(cl, map = new HashMap());
			}
			ObjectResolver<CompiledScript> resolver = null;
			Reference<ObjectResolver<CompiledScript>> ref = map.get(url);
			if (ref != null) {
				resolver = ref.get();
			}
			if (resolver == null) {
				resolver = ObjectResolver.newInstance(cl,
						new FunctionScriptFactory(cl));
				ref = new WeakReference<ObjectResolver<CompiledScript>>(
						resolver);
				map.put(url, ref);
			}
			return new EmbededScriptEngine(resolver, systemId, url);
		}
		return new EmbededScriptEngine(cl, systemId);
	}

	public static EmbededScriptEngine newInstance(ClassLoader cl, String code,
			String systemId) {
		return new EmbededScriptEngine(cl, code, systemId);
	}

	public static class ScriptResult {
		private Object result;

		public ScriptResult(Object result) {
			this.result = result;
		}

		public Object asObject() {
			return result;
		}

		public void asVoid() {
			asObject();
		}

		public Void asVoidObject() {
			asObject();
			return null;
		}

		public Set asSet() {
			return (Set) asObject();
		}

		public boolean asBoolean() {
			return ((Boolean) asObject()).booleanValue();
		}

		public char asChar() {
			return ((Character) asObject()).charValue();
		}

		public byte asByte() {
			return asNumberObject().byteValue();
		}

		public short asShort() {
			return asNumberObject().shortValue();
		}

		public int asInt() {
			return asNumberObject().intValue();
		}

		public long asLong() {
			return asNumberObject().longValue();
		}

		public float asFloat() {
			return asNumberObject().floatValue();
		}

		public double asDouble() {
			return asNumberObject().doubleValue();
		}

		public Number asNumberObject() {
			return (Number) asObject();
		}

		public Byte asByteObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Byte(number.byteValue());
		}

		public Short asShortObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Short(number.shortValue());
		}

		public Integer asIntegerObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Integer(number.intValue());
		}

		public Long asLongObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Long(number.longValue());
		}

		public Float asFloatObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Float(number.floatValue());
		}

		public Double asDoubleObject() {
			Number number = asNumberObject();
			if (number == null)
				return null;
			return new Double(number.doubleValue());
		}
	}

	private String code;
	private CompiledScript engine;
	private final String systemId;
	private final String filename;
	private final ObjectResolver<CompiledScript> resolver;
	private final EmbeddedScriptContext context;

	private EmbededScriptEngine(ObjectResolver<CompiledScript> resolver,
			String systemId, String filename) {
		this.systemId = systemId;
		this.filename = filename;
		this.resolver = resolver;
		this.context = new EmbeddedScriptContext();
	}

	private EmbededScriptEngine(ClassLoader cl, String systemId) {
		this.systemId = systemId;
		this.filename = systemId;
		this.context = new EmbeddedScriptContext();
		EmbeddedScriptFactory factory = new EmbeddedScriptFactory(cl, context);
		this.resolver = ObjectResolver.newInstance(cl, factory);
	}

	public EmbededScriptEngine(ClassLoader cl, String code, String systemId) {
		this(cl, systemId);
		this.code = code;
	}

	public EmbededScriptEngine importClass(String className) {
		context.importClass(className);
		return this;
	}

	public EmbededScriptEngine importPackage(String pkgName) {
		context.importPackage(pkgName);
		return this;
	}

	public EmbededScriptEngine assignRDFObject(String name, String uri) {
		context.assignRDFObject(name, uri);
		return this;
	}

	public EmbededScriptEngine returnType(Class<?> returnType) {
		context.setReturnType(returnType);
		return this;
	}

	public EmbededScriptEngine withThis() {
		context.setWithThis(true);
		return this;
	}

	public ScriptResult call(Object msg) {
		try {
			SimpleBindings bindings = new SimpleBindings();
			bindings.put("msg", msg);
			bindings.put("script", systemId);
			Object ret = getCompiledScript().eval(bindings);
			if (ret instanceof BehaviourException) {
				BehaviourException exc = (BehaviourException) ret;
				if (exc.getCause() instanceof RuntimeException)
					throw (RuntimeException) exc.getCause();
				if (exc.getCause() instanceof Error)
					throw (Error) exc.getCause();
				throw exc;
			}
			return new ScriptResult(ret);
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Exception e) {
			throw new BehaviourException(e);
		}
	}

	private synchronized CompiledScript getCompiledScript() throws Exception {
		if (engine != null)
			return engine;
		if (code != null) {
			try {
				StringReader in = new StringReader(code);
				return engine = resolver.getObjectFactory().create(filename, in);
			} catch (Exception e) {
				throw new ObjectCompositionException(e);
			}
		}
		return resolver.resolve(filename);
	}
}
