package org.openrdf.repository.object.composition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassTemplate {
	private Logger logger = LoggerFactory.getLogger(ClassTemplate.class);

	private CodeBuilder cb;

	private CtClass cc;

	private ClassFactory cp;

	protected ClassTemplate(final CtClass cc, final ClassFactory cp) {
		this.cc = cc;
		this.cp = cp;
		this.cb = new CodeBuilder(this) {
			@Override
			public CodeBuilder end() {
				try {
					semi();
					cc.makeClassInitializer().insertAfter(toString());
				} catch (CannotCompileException e) {
					throw new ObjectCompositionException(e.getMessage()
							+ " for " + toString(), e);
				}
				clear();
				return this;
			}
		};
	}

	public void addConstructor(Class<?>[] types, String string)
			throws ObjectCompositionException {
		try {
			CtConstructor con = new CtConstructor(asCtClassArray(types), cc);
			con.setBody("{" + string + "}");
			cc.addConstructor(con);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	@Override
	public String toString() {
		return cc.getName();
	}

	public void addInterface(Class<?> face) throws ObjectCompositionException {
		cc.addInterface(get(face));
	}

	public CodeBuilder assignStaticField(Class<?> type, final String fieldName)
			throws ObjectCompositionException {
		try {
			CtField field = new CtField(get(type), fieldName, cc);
			field.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
			cc.addField(field);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		}
		CodeBuilder code = new CodeBuilder(this) {
			@Override
			public CodeBuilder end() {
				semi();
				return cb.code(toString()).end();
			}
		};
		return code.assign(fieldName);
	}

	public void createField(Class<?> type, String fieldName)
			throws ObjectCompositionException {
		try {
			CtField field = new CtField(get(type), fieldName, cc);
			field.setModifiers(Modifier.PRIVATE);
			cc.addField(field);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder createMethod(Class<?> type, String name,
			Class<?>... parameters) throws ObjectCompositionException {
		CtClass[] exces = new CtClass[] { get(Throwable.class) };
		try {
			CtMethod cm = CtNewMethod.make(get(type), name,
					asCtClassArray(parameters), exces, null, cc);
			return begin(cm, parameters);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder createPrivateMethod(Class<?> type, String name,
			Class<?>... parameters) throws ObjectCompositionException {
		CtClass[] exces = new CtClass[] { get(Throwable.class) };
		try {
			CtMethod cm = CtNewMethod.make(Modifier.PRIVATE, get(type), name,
					asCtClassArray(parameters), exces, null, cc);
			return begin(cm, parameters);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder copyMethod(Method method, String name, boolean bridge)
			throws ObjectCompositionException {
		try {
			CtClass[] parameters = asCtClassArray(method.getParameterTypes());
			CtClass[] exces = new CtClass[] { get(Throwable.class) };
			CtMethod cm = CtNewMethod.make(get(method.getReturnType()), name,
					parameters, exces, null, cc);
			MethodInfo info = cm.getMethodInfo();
			copyAnnotations(method, info);
			if (bridge) {
				info.setAccessFlags(info.getAccessFlags() | AccessFlag.BRIDGE);
			}
			return begin(cm, method.getParameterTypes());
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder createTransientMethod(Method method)
			throws ObjectCompositionException {
		String name = method.getName();
		Class<?> type = method.getReturnType();
		Class<?>[] parameters = method.getParameterTypes();
		CtClass[] exces = new CtClass[] { get(Throwable.class) };
		try {
			CtMethod cm = CtNewMethod.make(get(type), name,
					asCtClassArray(parameters), exces, null, cc);
			cm.setModifiers(cm.getModifiers() | Modifier.TRANSIENT);
			MethodInfo info = cm.getMethodInfo();
			copyAnnotations(method, info);
			info.setAccessFlags(info.getAccessFlags() | AccessFlag.BRIDGE);
			return begin(cm, parameters);
		} catch (CannotCompileException e) {
			throw new ObjectCompositionException(e);
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder getCodeBuilder() {
		return cb;
	}

	public CtClass getCtClass() {
		return cc;
	}

	public Set<String> getDeclaredFieldNames() {
		CtField[] fields = cc.getDeclaredFields();
		Set<String> result = new HashSet<String>(fields.length);
		for (CtField field : fields) {
			result.add(field.getName());
		}
		return result;
	}

	public Class<?> getSuperclass() {
		try {
			return cp.getJavaClass(cc.getSuperclass());
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		} catch (ClassNotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public Class<?>[] getInterfaces() throws ObjectCompositionException {
		try {
			CtClass[] cc1 = cc.getInterfaces();
			Class<?>[] result = new Class<?>[cc1.length];
			for (int i = 0; i < cc1.length; i++) {
				result[i] = cp.getJavaClass(cc1[i]);
			}
			return result;
		} catch (NotFoundException e) {
			throw new ObjectCompositionException(e);
		} catch (ClassNotFoundException e) {
			throw new ObjectCompositionException(e);
		}
	}

	public CodeBuilder overrideMethod(Method method, boolean bridge)
			throws ObjectCompositionException {
		return copyMethod(method, method.getName(), bridge);
	}

	public Set<Field> getFieldsRead(Method method) throws NotFoundException {
		String name = method.getName();
		CtClass[] parameters = asCtClassArray(method.getParameterTypes());
		final Set<CtMethod> methods = new HashSet<CtMethod>();
		final Set<Field> accessed = new HashSet<Field>();
		for (CtMethod cm : cc.getMethods()) {
			if (equals(cm, name, parameters)) {
				findMethodCalls(cm, methods);
			}
		}
		for (CtMethod cm : methods) {
			try {
				cm.instrument(new ExprEditor() {
					@Override
					public void edit(FieldAccess f) {
						try {
							if (f.isReader()) {
								CtField field = f.getField();
								String name = field.getName();
								String dname = field.getDeclaringClass()
										.getName();
								Class<?> declared = cp.loadClass(dname);
								accessed.add(declared.getDeclaredField(name));
							}
						} catch (RuntimeException exc) {
							throw exc;
						} catch (Exception exc) {
							logger.warn(exc.toString(), exc);
						}
					}
				});
			} catch (CannotCompileException e) {
				throw new AssertionError(e);
			}
		}
		return accessed;
	}

	public Set<Field> getFieldsWritten(Method method) throws NotFoundException {
		String name = method.getName();
		CtClass[] parameters = asCtClassArray(method.getParameterTypes());
		final Set<CtMethod> methods = new HashSet<CtMethod>();
		final Set<Field> accessed = new HashSet<Field>();
		for (CtMethod cm : cc.getMethods()) {
			if (equals(cm, name, parameters)) {
				findMethodCalls(cm, methods);
			}
		}
		for (CtMethod cm : methods) {
			try {
				cm.instrument(new ExprEditor() {
					@Override
					public void edit(FieldAccess f) {
						try {
							if (f.isWriter()) {
								CtField field = f.getField();
								String name = field.getName();
								String dname = field.getDeclaringClass()
										.getName();
								Class<?> declared = cp.loadClass(dname);
								accessed.add(declared.getDeclaredField(name));
							}
						} catch (RuntimeException exc) {
							throw exc;
						} catch (Exception exc) {
							logger.warn(exc.toString(), exc);
						}
					}
				});
			} catch (CannotCompileException e) {
				throw new AssertionError(e);
			}
		}
		return accessed;
	}

	public ClassTemplate loadClassTemplate(Class<?> class1) {
		return new ClassTemplate(get(class1), cp);
	}

	CtClass get(Class<?> type) throws ObjectCompositionException {
		if (type.isPrimitive()) {
			return getPrimitive(type);
		}
		try {
			if (type.isArray())
				return Descriptor.toCtClass(type.getName(), cc.getClassPool());
			return cc.getClassPool().get(type.getName());
		} catch (NotFoundException e) {
			try {
				cp.appendClassLoader(type.getClassLoader());
				if (type.isArray())
					return Descriptor.toCtClass(type.getName(), cc
							.getClassPool());
				return cc.getClassPool().get(type.getName());
			} catch (NotFoundException e1) {
				throw new ObjectCompositionException(e);
			}
		}
	}

	private Set<CtMethod> getAll(Method method) throws NotFoundException {
		Set<CtMethod> result = new HashSet<CtMethod>();
		String name = method.getName();
		CtClass[] parameters = asCtClassArray(method.getParameterTypes());
		for (CtMethod cm : get(method.getDeclaringClass()).getDeclaredMethods()) {
			if (!equals(cm, name, parameters))
				continue;
			result.add(cm);
		}
		return result;
	}

	private boolean equals(CtMethod cm, String name, CtClass[] parameters)
			throws NotFoundException {
		return cm.getName().equals(name)
				&& Arrays.equals(cm.getParameterTypes(), parameters);
	}

	private CtClass getPrimitive(Class<?> type) {
		if (type.equals(Boolean.TYPE))
			return CtClass.booleanType;
		if (type.equals(Byte.TYPE))
			return CtClass.byteType;
		if (type.equals(Character.TYPE))
			return CtClass.charType;
		if (type.equals(Double.TYPE))
			return CtClass.doubleType;
		if (type.equals(Float.TYPE))
			return CtClass.floatType;
		if (type.equals(Integer.TYPE))
			return CtClass.intType;
		if (type.equals(Long.TYPE))
			return CtClass.longType;
		if (type.equals(Short.TYPE))
			return CtClass.shortType;
		if (type.equals(Void.TYPE))
			return CtClass.voidType;
		throw new ObjectCompositionException("Unknown primative type: "
				+ type.getName());
	}

	private void findMethodCalls(CtMethod cm, final Set<CtMethod> methods) {
		if (methods.add(cm)) {
			try {
				cm.instrument(new ExprEditor() {
					@Override
					public void edit(MethodCall m) {
						try {
							CtClass enclosing = m.getEnclosingClass();
							String className = m.getClassName();
							if (isAssignableFrom(className, enclosing)) {
								findMethodCalls(m.getMethod(), methods);
							}
						} catch (NotFoundException e) {
							logger.warn(e.toString(), e);
						}
					}

					private boolean isAssignableFrom(String className,
							CtClass enclosing) throws NotFoundException {
						if (enclosing == null)
							return false;
						if (className.equals(enclosing.getName()))
							return true;
						return isAssignableFrom(className, enclosing
								.getSuperclass());
					}
				});
			} catch (CannotCompileException e) {
				throw new AssertionError(e);
			}
		}
	}

	private void copyAnnotations(Method method, MethodInfo info)
			throws NotFoundException {
		copyMethodAnnotations(method, info);
		copyParameterAnnotations(method, info);
	}

	private void copyMethodAnnotations(Method method, MethodInfo info)
			throws NotFoundException {
		for (CtMethod e : getAll(method)) {
			MethodInfo em = e.getMethodInfo();
			AnnotationsAttribute ai = (AnnotationsAttribute) em
					.getAttribute(AnnotationsAttribute.visibleTag);
			if (ai == null)
				continue;
			if (ai.getAnnotations().length > 0) {
				info.addAttribute(ai.copy(info.getConstPool(),
						Collections.EMPTY_MAP));
				break;
			}
		}
	}

	private void copyParameterAnnotations(Method method, MethodInfo info)
			throws NotFoundException {
		for (CtMethod e : getAll(method)) {
			MethodInfo em = e.getMethodInfo();
			ParameterAnnotationsAttribute ai = (ParameterAnnotationsAttribute) em
					.getAttribute(ParameterAnnotationsAttribute.visibleTag);
			if (ai == null)
				continue;
			Annotation[][] anns = ai.getAnnotations();
			for (int i = 0, n = anns.length; i < n; i++) {
				if (anns[i].length > 0) {
					info.addAttribute(ai.copy(info.getConstPool(),
							Collections.EMPTY_MAP));
					return;
				}
			}
		}
	}

	private CtClass[] asCtClassArray(Class<?>[] cc) throws NotFoundException {
		CtClass[] result = new CtClass[cc.length];
		for (int i = 0; i < cc.length; i++) {
			result[i] = get(cc[i]);
		}
		return result;
	}

	private CodeBuilder begin(final CtMethod cm, Class<?>... parameters) {
		CodeBuilder cb = new CodeBuilder(this) {
			@Override
			public CodeBuilder end() {
				code("}");
				CtClass cc = cm.getDeclaringClass();
				try {
					int mod = cm.getModifiers();
					mod = Modifier.clear(mod, Modifier.ABSTRACT);
					mod = Modifier.clear(mod, Modifier.NATIVE);
					cm.setModifiers(mod);
					cm.setBody(toString());
					cc.addMethod(cm);
				} catch (Exception e) {
					StringBuilder sb = new StringBuilder();
					try {
						for (CtClass inter : cc.getInterfaces()) {
							sb.append(inter.getSimpleName()).append(" ");
						}
					} catch (NotFoundException e2) {
					}
					String sn = cc.getSimpleName();
					System.err.println(sn + " implements " + sb);
					throw new ObjectCompositionException(e.getMessage()
							+ " for " + toString(), e);
				}
				clear();
				return this;
			}
		};
		return cb.code("{");
	}

}
