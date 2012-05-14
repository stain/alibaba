package org.openrdf.repository.object.advice;

import java.lang.reflect.Method;

public interface AdviceFactory {
	Advice createAdviser(Method method);
}
