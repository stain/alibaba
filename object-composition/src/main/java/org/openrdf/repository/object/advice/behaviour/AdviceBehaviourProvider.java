package org.openrdf.repository.object.advice.behaviour;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.advice.AdviceFactory;
import org.openrdf.repository.object.advice.AdviceService;
import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.BehaviourProvider;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.exceptions.ObjectCompositionException;
import org.openrdf.repository.object.managers.PropertyMapper;

public class AdviceBehaviourProvider implements BehaviourProvider {
	private final AdviceService service = AdviceService.newInstance();

	public void setClassDefiner(ClassFactory definer) {
		// TODO Auto-generated method stub

	}

	public void setBaseClasses(Set<Class<?>> bases) {
		// TODO Auto-generated method stub

	}

	public void setPropertyMapper(PropertyMapper mapper) {
		// TODO Auto-generated method stub

	}

	public Collection<? extends BehaviourFactory> getBehaviourFactories(
			Collection<Class<?>> classes) throws ObjectCompositionException {
		List<AdviceBehaviourFactory> list = new ArrayList<AdviceBehaviourFactory>();
		for (Class<?> cls : classes) {
			addAdvisers(cls, list);
		}
		return list;
	}

	private void addAdvisers(Class<?> cls, List<AdviceBehaviourFactory> list) {
		for (Method method : cls.getDeclaredMethods()) {
			if (isPublicOrProtected(method)) {
				addAdvisers(method, list);
			}
		}
		for (Class<?> face : cls.getInterfaces()) {
			addAdvisers(face, list);
		}
		if (cls.getSuperclass() != null) {
			addAdvisers(cls.getSuperclass(), list);
		}
	}

	private boolean isPublicOrProtected(Method method) {
		int modifiers = method.getModifiers();
		return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
	}

	private void addAdvisers(Method method, List<AdviceBehaviourFactory> list) {
		for (Annotation ann : method.getAnnotations()) {
			Class<? extends Annotation> t = ann.annotationType();
			AdviceFactory f = service.getAdviserFactory(t);
			if (f != null) {
				Advice a = f.createAdviser(method);
				list.add(new AdviceBehaviourFactory(a, method, t));
			}
		}
	}

}
