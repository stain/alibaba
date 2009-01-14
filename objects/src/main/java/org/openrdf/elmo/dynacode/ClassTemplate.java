package org.openrdf.elmo.dynacode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
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
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import org.openrdf.elmo.exceptions.ElmoCompositionException;
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
					throw new ElmoCompositionException(e.getMessage() + " for "
							+ toString(), e);
				}
				clear();
				return this;
			}
		};
	}

	public void addConstructor(Class<?>[] types, String string)
			throws ElmoCompositionException {
		try {
			CtConstructor con = new CtConstructor(asCtClassArray(types), cc);
			con.setBody("{" + string + "}");
			cc.addConstructor(con);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
		} catch (NotFoundException e) {
			throw new ElmoCompositionException(e);
		}
	}

	@Override
	public String toString() {
		return cc.getName();
	}

	public void addInterface(Class<?> face) throws ElmoCompositionException {
		cc.addInterface(get(face));
	}

	public CodeBuilder assignStaticField(Class<?> type, final String fieldName)
			throws ElmoCompositionException {
		try {
			CtField field = new CtField(get(type), fieldName, cc);
			field.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
			cc.addField(field);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
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
			throws ElmoCompositionException {
		try {
			CtField field = new CtField(get(type), fieldName, cc);
			field.setModifiers(Modifier.PRIVATE);
			cc.addField(field);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
		}
	}

	public CodeBuilder createMethod(Class<?> type, String name,
			Class<?>... parameters) throws ElmoCompositionException {
		CtClass[] exces = new CtClass[] { get(Throwable.class) };
		try {
			CtMethod cm = CtNewMethod.make(get(type), name,
					asCtClassArray(parameters), exces, null, cc);
			MethodInfo info = cm.getMethodInfo();
			info.setAccessFlags(info.getAccessFlags() | AccessFlag.BRIDGE);
			return begin(cm, parameters);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
		} catch (NotFoundException e) {
			throw new ElmoCompositionException(e);
		}
	}

	public CodeBuilder createTransientMethod(Class<?> type, String name,
			Class<?>... parameters) throws ElmoCompositionException {
		CtClass[] exces = new CtClass[] { get(Throwable.class) };
		try {
			CtMethod cm = CtNewMethod.make(get(type), name,
					asCtClassArray(parameters), exces, null, cc);
			cm.setModifiers(cm.getModifiers() | Modifier.TRANSIENT);
			MethodInfo info = cm.getMethodInfo();
			info.setAccessFlags(info.getAccessFlags() | AccessFlag.BRIDGE);
			return begin(cm, parameters);
		} catch (CannotCompileException e) {
			throw new ElmoCompositionException(e);
		} catch (NotFoundException e) {
			throw new ElmoCompositionException(e);
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
			throw new ElmoCompositionException(e);
		} catch (ClassNotFoundException e) {
			throw new ElmoCompositionException(e);
		}
	}

	public Class<?>[] getInterfaces() throws ElmoCompositionException {
		try {
			CtClass[] cc1 = cc.getInterfaces();
			Class<?>[] result = new Class<?>[cc1.length];
			for (int i = 0; i < cc1.length; i++) {
				result[i] = cp.getJavaClass(cc1[i]);
			}
			return result;
		} catch (NotFoundException e) {
			throw new ElmoCompositionException(e);
		} catch (ClassNotFoundException e) {
			throw new ElmoCompositionException(e);
		}
	}

	public CodeBuilder overrideMethod(Method method)
			throws ElmoCompositionException {
		return createMethod(method.getReturnType(), method.getName(), method
				.getParameterTypes());
	}

	public Set<Field> getAccessedFields(Method method)
			throws NotFoundException {
		String name = method.getName();
		CtClass[] parameters = asCtClassArray(method.getParameterTypes());
		final Set<CtMethod> methods = new HashSet<CtMethod>();
		final Set<Field> accessed = new HashSet<Field>();
		for (CtMethod cm : cc.getMethods()) {
			if (cm.getName().equals(name)
					&& Arrays.equals(cm.getParameterTypes(), parameters)) {
				findMethodCalls(cm, methods);
			}
		}
		for (CtMethod cm : methods) {
			try {
				cm.instrument(new ExprEditor() {
					@Override
					public void edit(FieldAccess f) {
						try {
							CtField field = f.getField();
							String name = field.getName();
							String dname = field.getDeclaringClass().getName();
							Class<?> declared = cp.loadClass(dname);
							accessed.add(declared.getDeclaredField(name));
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

	CtClass get(Class<?> type) throws ElmoCompositionException {
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
					return Descriptor.toCtClass(type.getName(), cc.getClassPool());
				return cc.getClassPool().get(type.getName());
			} catch (NotFoundException e1) {
				throw new ElmoCompositionException(e);
			}
		}
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
		throw new ElmoCompositionException("Unknown primative type: "
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
						return isAssignableFrom(className, enclosing.getSuperclass());
					}
				});
			} catch (CannotCompileException e) {
				throw new AssertionError(e);
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
					throw new ElmoCompositionException(e.getMessage() + " for "
							+ toString(), e);
				}
				clear();
				return this;
			}
		};
		return cb.code("{");
	}

}
