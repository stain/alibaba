package org.openrdf.repository.object.advice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdviceService {
	private static final ServiceLoader<AdviceProvider> installed = ServiceLoader
			.load(AdviceProvider.class, AdviceService.class.getClassLoader());

	public static AdviceService newInstance() {
		return newInstance(Thread.currentThread().getContextClassLoader());
	}

	public static AdviceService newInstance(ClassLoader cl) {
		return new AdviceService(cl);
	}

	private final Logger logger = LoggerFactory.getLogger(AdviceService.class);
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
			AdviceFactory factory;
			factory = getAdviceFactory(annotationType, loader);
			if (factory != null) {
				factories.put(annotationType, factory);
				return factory;
			}
			factory = getAdviceFactory(annotationType, installed);
			if (factory != null) {
				factories.put(annotationType, factory);
				return factory;
			}
			factories.put(annotationType, null);
			return null;
		}
	}

	private AdviceFactory getAdviceFactory(Class<?> type,
			ServiceLoader<AdviceProvider> loader) {
		Iterator<AdviceProvider> iter = loader.iterator();
		while (iter.hasNext()) {
			try {
				AdviceFactory f = iter.next().getAdviserFactory(type);
				if (f != null) {
					factories.put(type, f);
					return f;
				}
			} catch (ServiceConfigurationError e) {
				logger.warn(e.getMessage());
			}
		}
		return null;
	}
}
