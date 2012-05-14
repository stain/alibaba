package org.openrdf.repository.object.composition.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.BehaviourProvider;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.managers.PropertyMapper;

public class BehaviourProviderService {
	public static BehaviourProviderService newInstance(ClassFactory cl) {
		return new BehaviourProviderService(cl);
	}

	private final ServiceLoader<BehaviourProvider> loader;
	private final ClassFactory cl;

	public BehaviourProviderService(ClassFactory cl) {
		this.cl = cl;
		this.loader = ServiceLoader.load(BehaviourProvider.class, cl);
	}

	public Collection<BehaviourFactory> findImplementations(PropertyMapper mapper,
			Collection<Class<?>> classes, Set<Class<?>> bases)
			throws IOException {
		List<BehaviourFactory> implementations = new ArrayList<BehaviourFactory>();
		for (BehaviourProvider bf : loader) {
			bf.setClassDefiner(cl);
			bf.setPropertyMapper(mapper);
			bf.setBaseClasses(bases);
			implementations.addAll(bf.getBehaviourFactories(classes));
		}
		return implementations;
	}

}
