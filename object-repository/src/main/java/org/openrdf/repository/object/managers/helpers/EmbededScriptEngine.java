package org.openrdf.repository.object.managers.helpers;

import static org.openrdf.repository.object.RDFObject.GET_CONNECTION;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.openrdf.repository.object.concepts.Message;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.util.ObjectResolver;
import org.openrdf.repository.object.util.ObjectResolver.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbededScriptEngine {
	private static final String BEHAVIOUR = BehaviourException.class.getName();
	private static final Pattern KEYWORDS = Pattern.compile("(?:var\\s+|\\.)(break|case|catch|const|continue|default|delete|do|else|export|finally|for|function|if|in|instanceof|import|name|new|return|switch|this|throw|try|typeof|var|void|while|with)\b");
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
		primitives.put(Void.TYPE, null);
		// load the script engine now, to import any binary libraries
		if (null == new ScriptEngineManager().getEngineByName("ECMAScript"))
			throw new AssertionError("ECMAScript not available");
	}

	public class ScriptCallBuilder {
		private CompiledScript engine;
		private Message msg;

		public ScriptCallBuilder(CompiledScript engine, Message msg) {
			this.engine = engine;
			this.msg = msg;
		}

		public Object asObject() {
			try {
				SimpleScriptContext context = new SimpleScriptContext();
				context.setWriter(new OutputStreamWriter(System.out));
				context.setErrorWriter(new OutputStreamWriter(System.err));
				SimpleBindings bindings = new SimpleBindings();
				bindings.put("msg", msg);
				context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
				Object ret = engine.eval(context);
				if (ret instanceof BehaviourException) {
					BehaviourException exc = (BehaviourException) ret;
					if (exc.getCause() instanceof RuntimeException)
						throw (RuntimeException) exc.getCause();
					if (exc.getCause() instanceof Error)
						throw (Error) exc.getCause();
					throw exc;
				}
				return ret;
			} catch (ScriptException e) {
				throw new BehaviourException(e);
			}
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

	private Logger logger = LoggerFactory.getLogger(EmbededScriptEngine.class);
	private String code;
	private CompiledScript engine;
	private final String systemId;
	private final ObjectResolver<CompiledScript> resolver;
	private Set<String> classes = new HashSet<String>();
	private Set<String> packages = new HashSet<String>();
	private Map<String, String> assignments = new HashMap<String, String>();
	private Class<?> returnType = Object.class;
	private boolean withThis;

	public EmbededScriptEngine(final ClassLoader cl, String systemId) {
		this.systemId = systemId;
		resolver = ObjectResolver.newInstance(cl,
				new ObjectFactory<CompiledScript>() {
					public CompiledScript create(String systemId, InputStream in)
							throws Exception {
						return create(systemId, new InputStreamReader(in,
								"UTF-8"));
					}

					public CompiledScript create(String systemId, Reader in)
							throws Exception {
						try {
							return createEngine(cl, systemId, read(in));
						} finally {
							in.close();
						}
					}

					public String[] getContentTypes() {
						return new String[] { "text/javascript",
								"application/javascript", "text/ecmascript",
								"application/ecmascript" };
					}

					public boolean isReusable() {
						return true;
					}
				});
	}

	public EmbededScriptEngine(ClassLoader cl, String code, String systemId) {
		this(cl, systemId);
		this.code = code;
	}

	public EmbededScriptEngine importClass(String className) {
		warnIfKeywordUsed(className);
		classes.add(className);
		return this;
	}

	public EmbededScriptEngine importPackage(String pkgName) {
		warnIfKeywordUsed(pkgName);
		packages.add(pkgName);
		return this;
	}

	public EmbededScriptEngine assignRDFObject(String name, String uri) {
		warnIfKeywordUsed(name);
		assignments.put(name, uri);
		return this;
	}

	public EmbededScriptEngine returnType(Class<?> returnType) {
		this.returnType = returnType;
		return this;
	}

	public EmbededScriptEngine withThis() {
		this.withThis = true;
		return this;
	}

	public ScriptCallBuilder call(Message msg) {
		try {
			return new ScriptCallBuilder(getScriptEngine(), msg);
		} catch (Exception e) {
			throw new ObjectCompositionException(e);
		}
	}

	private void warnIfKeywordUsed(String code) {
		Matcher m = KEYWORDS.matcher(code);
		if (m.find()) {
			logger.warn("{} is a ECMA script keyword", m.group(1));
		}
	}

	private CompiledScript getScriptEngine() throws Exception {
		if (engine != null)
			return engine;
		if (code != null) {
			try {
				return engine = resolver.getObjectFactory().create(systemId, new StringReader(code));
			} catch (Exception e) {
				throw new ObjectCompositionException(e);
			}
		}
		return resolver.resolve(systemId);
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		for (String pkg : packages) {
			out.write("importPackage(Packages.");
			out.write(pkg);
			out.write("); ");
		}
		for (String className : classes) {
			out.write("importClass(Packages.");
			out.write(className);
			out.write("); ");
		}
		out.write("(function(msg) { try{ ");
		for (Map.Entry<String, String> e : assignments.entrySet()) {
			out.write("var ");
			out.write(e.getKey());
			out.write(" = msg.getMsgTarget().");
			out.write(GET_CONNECTION);
			out.write("().getObject(\"");
			out.write(e.getValue());
			out.write("\"); ");
		}
		if (withThis) {
			out.write("with(this) { ");
		}
		out.write("with(msg) { ");
		out.write("function proceed() { ");
		if (Void.TYPE.equals(returnType)) {
			out.write("msg.");
			out.write(Message.PROCEED);
			out.write("(); ");
			out.write("return null; ");
		} else {
			out.write("return msg.");
			out.write(Message.PROCEED);
			out.write("()");
			if (returnType.isPrimitive()) {
				out.write(primitives.get(returnType));
			}
			out.write("; ");
		}
		out.write("} ");
		int read;
		char[] cbuf = new char[1024];
		while ((read = in.read(cbuf)) >= 0) {
			out.write(cbuf, 0, read);
		}
		out.write("\n\t\t");
		out.write("}\n\t");
		if (withThis) {
			out.write("}\n\t");
		}
		out.write("} catch (e if e instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e, \"").append(systemId).append("\");\n\t");
		out
				.write("} catch (e if e.javaException instanceof java.lang.Throwable) {\n\t\t");
		out.append("return new Packages.").append(BEHAVIOUR);
		out.append("(e.javaException, \"").append(systemId).append("\");\n\t");
		out.write("}\n");
		out.write("}).call(msg.msgTarget, msg);\n");
		return out.toString();
	}

	private CompiledScript createEngine(ClassLoader cl, String systemId,
			String code) throws ObjectStoreConfigException {
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
