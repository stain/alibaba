package org.openrdf.elmo.dynacode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.bytecode.Descriptor;

import org.openrdf.repository.object.exceptions.ElmoCompositionException;

public abstract class CodeBuilder {
	private StringBuilder body = new StringBuilder();

	private ClassTemplate klass;

	private Map<String, Map<List<Class<?>>, String>> methodTemplateVars = new HashMap();

	private Map<Method, String> methodVars = new HashMap();

	private int varCounter;

	protected CodeBuilder(ClassTemplate klass) {
		super();
		this.klass = klass;
	}

	public CodeBuilder assign(String var) {
		body.append(var).append(" = ");
		return this;
	}

	public CodeBuilder castObject(String field, Class<?> type) {
		body.append("(");
		if (type.isPrimitive()) {
			body.append(getPrimitiveWrapper(type));
			body.append(")").append(field);
		} else {
			body.append(getJavaClassCodeNameOf(type)).append(")").append(field);
		}
		return this;
	}

	public CodeBuilder code(String str) {
		body.append(str);
		return this;
	}

	public CodeBuilder codeInstanceof(String field, Class<?> type) {
		body.append(field).append(" instanceof ");
		body.append(getJavaClassCodeNameOf(type));
		return this;
	}

	public CodeBuilder codeObject(String field, Class<?> type) {
		if (type.isPrimitive()) {
			body.append(getPrimitiveWrapper(type));
			body.append(".valueOf(").append(field).append(")");
		} else {
			body.append(field);
		}
		return this;
	}

	public CodeBuilder construct(Class<?> javaClass, Object... args) {
		body.append("new ").append(javaClass.getName()).append("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				code(",");
			}
			insert(args[i]);
		}
		body.append(")");
		return this;
	}

	public CodeBuilder staticInvoke(Method method, Object... args) {
		code(method.getDeclaringClass().getName());
		code(".").code(method.getName()).code("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				code(",");
			}
			insert(args[i]);
		}
		code(")");
		return this;
	}

	public CodeBuilder declareObject(Class<?> type, String var) {
		code(getJavaClassCodeNameOf(type));
		return code(" ").assign(var);
	}

	public CodeBuilder declareWrapper(Class<?> type, String var) {
		if (type.isPrimitive()) {
			code(getPrimitiveWrapper(type));
		} else {
			code(getJavaClassCodeNameOf(type));
		}
		return code(" ").assign(var);
	}

	public abstract CodeBuilder end();

	public CodeBuilder insert(boolean b) {
		body.append(b);
		return this;
	}

	public CodeBuilder insert(char c) {
		body.append("'").append(c).append("'");
		return this;
	}

	public CodeBuilder insert(Class<?> javaClass) {
		body.append(getJavaClassObjectCode(javaClass));
		return this;
	}

	public CodeBuilder insert(double d) {
		body.append(d);
		return this;
	}

	public CodeBuilder insert(float f) {
		body.append(f);
		return this;
	}

	public CodeBuilder insert(int i) {
		body.append(i);
		return this;
	}

	public CodeBuilder insert(long lng) {
		body.append(lng);
		return this;
	}

	public CodeBuilder insert(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();
		String name = method.getName();
		Class<?>[] params = method.getParameterTypes();
		CodeBuilder cb = klass.getCodeBuilder();
		String var = cb.methodVars.get(method);
		if (var == null) {
			var = cb.getVarName("Method");
		} else {
			body.append(var);
			return this;
		}
		String before = toString();
		clear();
		String parameterTypes = declareVar(params, cb);
		CodeBuilder field = klass.assignStaticField(Method.class, var);
		field.insert(declaringClass);
		field.code(".getDeclaredMethod(").insert(name);
		field.code(", ").code(parameterTypes).code(")").end();
		methodVars.put(method, var);
		code(before);
		body.append(var);
		return this;
	}

	public CodeBuilder insert(Object o) {
		if (o == null) {
			body.append("null");
		} else {
			visit(o, o.getClass());
		}
		return this;
	}

	public CodeBuilder insert(String str) {
		body.append("\"").append(str).append("\"");
		return this;
	}

	public CodeBuilder insert(Class<?>[] params) {
		CodeBuilder cb = klass.getCodeBuilder();
		String parameterTypes = declareVar(params, cb);
		String var = cb.getVarName("Classes");
		CodeBuilder field = klass.assignStaticField(params.getClass(), var);
		field.code(parameterTypes).end();
		body.append(var);
		return this;
	}

	public CodeBuilder insertMethod(String name, Class<?>[] params) {
		List<Class<?>> list = Arrays.asList(params);
		CodeBuilder cb = klass.getCodeBuilder();
		Map<List<Class<?>>, String> map = cb.methodTemplateVars.get(name);
		if (map == null) {
			cb.methodTemplateVars.put(name, map = new HashMap());
		} else {
			if (map.containsKey(list)) {
				body.append(map.get(list));
				return this;
			}
		}
		String parameterTypes = declareVar(params, cb);
		String var = cb.getVarName("Method");
		CodeBuilder field = klass.assignStaticField(Method.class, var);
		field.insert(klass.getCtClass());
		field.code(".getDeclaredMethod(").insert(name);
		field.code(", ").code(parameterTypes).code(")").end();
		map.put(list, var);
		body.append(var);
		return this;
	}

	public int length() {
		return body.length();
	}

	public CodeBuilder semi() {
		body.append(";\n");
		return this;
	}

	@Override
	public String toString() {
		return body.toString();
	}

	protected void clear() {
		body.delete(0, length());
	}

	private CharSequence getJavaClassObjectCode(Class<?> type) {
		CtClass cc = klass.get(type);
		return getJavaClassObjectCode(cc);
	}

	private String declareVar(Class<?>[] classes, CodeBuilder cb) {
		String var = cb.getVarName("Classes");
		cb.code("java.lang.Class[] ").code(var);
		cb.code(" = ").code("new java.lang.Class[");
		cb.insert(classes.length).code("]").code(";\n");
		for (int i = 0; i < classes.length; i++) {
			cb.code(var).code("[").insert(i).code("]");
			cb.code(" = ");
			cb.insert(classes[i]);
			cb.code(";\n");
		}
		return var;
	}

	private String getJavaClassCodeNameOf(Class<?> type) {
		return klass.get(type).getName();
	}

	private CharSequence getJavaClassObjectCode(CtClass cc) {
		StringBuilder body = new StringBuilder();
		if (cc.isPrimitive()) {
			return body.append(getPrimitiveJavaClassWrapper(cc).getName())
					.append(".TYPE");
		}
		body.append(Class.class.getName());
		body.append(".forName(\"");
		String name = Descriptor.toJavaName(Descriptor.toJvmName(cc));
		body.append(name);
		body.append("\")");
		return body;
	}

	private Class<?> getPrimitiveJavaClassWrapper(CtClass cc) {
		if (cc.equals(CtClass.booleanType))
			return Boolean.class;
		if (cc.equals(CtClass.byteType))
			return Byte.class;
		if (cc.equals(CtClass.charType))
			return Character.class;
		if (cc.equals(CtClass.doubleType))
			return Double.class;
		if (cc.equals(CtClass.floatType))
			return Float.class;
		if (cc.equals(CtClass.intType))
			return Integer.class;
		if (cc.equals(CtClass.longType))
			return Long.class;
		if (cc.equals(CtClass.shortType))
			return Short.class;
		throw new AssertionError();
	}

	private String getPrimitiveWrapper(Class<?> type) {
		String wrap;
		if (boolean.class.equals(type)) {
			wrap = Boolean.class.getName();
		} else if (char.class.equals(type)) {
			wrap = Character.class.getName();
		} else if (int.class.equals(type)) {
			wrap = Integer.class.getName();
		} else {
			String prim = type.getName();
			wrap = Character.toUpperCase(prim.charAt(0)) + prim.substring(1);

		}
		return wrap;
	}

	private String getVarName(String type) {
		return "_$" + type + varCounter++;
	}

	private CodeBuilder insert(CtClass ctClass) {
		body.append(getJavaClassObjectCode(ctClass));
		return this;
	}

	private boolean visit(Object o, Class oc) {
		try {
			Class c = getClass();
			Class[] args = new Class[] { oc };
			Method m = c.getMethod("insert", args);
			m.invoke(this, o);
			return true;
		} catch (NoSuchMethodException e) {
			Class sc = oc.getSuperclass();
			if (sc != null && !Object.class.equals(sc)) {
				if (visit(o, sc))
					return true;
			}
			for (Class face : oc.getInterfaces()) {
				if (visit(o, face))
					return true;
			}
			return false;
		} catch (IllegalArgumentException e) {
			throw new ElmoCompositionException(e);
		} catch (IllegalAccessException e) {
			throw new ElmoCompositionException(e);
		} catch (InvocationTargetException e) {
			throw new ElmoCompositionException(e);
		}
	}
}
