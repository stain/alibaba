package org.openrdf.repository.object.advice;

public interface AdviceProvider {
	AdviceFactory getAdviserFactory(Class<?> annotationType);
}
