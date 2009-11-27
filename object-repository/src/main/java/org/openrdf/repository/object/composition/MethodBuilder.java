package org.openrdf.repository.object.composition;

import static javassist.bytecode.AnnotationsAttribute.visibleTag;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;

import org.openrdf.repository.object.exceptions.ObjectCompositionException;

public class MethodBuilder extends CodeBuilder {
	private ClassTemplate klass;
	private CtMethod cm;

	protected MethodBuilder(ClassTemplate klass, CtMethod cm) {
		super(klass);
		this.klass = klass;
		this.cm = cm;
		code("{");
	}

	public MethodBuilder ann(Class<?> type, Class<?>... values) {
		MethodInfo info = cm.getMethodInfo();
		ConstPool cp = info.getConstPool();
		ClassMemberValue[] elements = new ClassMemberValue[values.length];
		for (int i = 0; i < values.length; i++) {
			elements[i] = createClassMemberValue((Class<?>) values[i], cp);
		}
		ArrayMemberValue value = new ArrayMemberValue(cp);
		value.setValue(elements);
		AnnotationsAttribute ai = (AnnotationsAttribute) info
				.getAttribute(visibleTag);
		if (ai == null) {
			ai = new AnnotationsAttribute(cp, visibleTag);
			info.addAttribute(ai);
		}
		try {
			Annotation annotation = new Annotation(cp, klass.get(type));
			annotation.addMemberValue("value", value);
			ai.addAnnotation(annotation);
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
		return this;
	}

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
			throw new ObjectCompositionException(e.getMessage() + " for "
					+ toString(), e);
		}
		clear();
		return this;
	}

}
