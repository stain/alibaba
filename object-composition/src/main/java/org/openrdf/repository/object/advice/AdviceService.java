package org.openrdf.repository.object.advice;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class AdviceService {
	private static final ServiceLoader<AdviceProvider> installed = ServiceLoader
			.load(AdviceProvider.class, AdviceService.class.getClassLoader());

	public static AdviceService newInstance() {
		return newInstance(Thread.currentThread().getContextClassLoader());
	}

	public static AdviceService newInstance(ClassLoader cl) {
		return new AdviceService(cl);
	}

	private final ServiceLoader<AdviceProvider> loader;
	private final Map<Class<?>, AdviceFactory> factories = new HashMap<Class<?>, AdviceFactory>();

	public AdviceService(ClassLoader cl) {
		this.loader = ServiceLoader.load(AdviceProvider.class,
				AdviceService.class.getClassLoader());
	}

	public synchronized AdviceFactory getAdviserFactory(Class<?> annotationType) {
		if (factories.containsKey(annotationType)) {
			return factories.get(annotationType);
		} else {
			for (AdviceProvider provider : loader) {
				AdviceFactory f = provider.getAdviserFactory(annotationType);
				if (f != null) {
					factories.put(annotationType, f);
					return f;
				}
			}
			for (AdviceProvider provider : installed) {
				AdviceFactory f = provider.getAdviserFactory(annotationType);
				if (f != null) {
					factories.put(annotationType, f);
					return f;
				}
			}
			factories.put(annotationType, null);
			return null;
		}
	}
}
