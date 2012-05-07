package org.openrdf.repository.object.composition.helpers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.openrdf.repository.object.composition.BehaviourFactory;
import org.openrdf.repository.object.composition.BehaviourProvider;
import org.openrdf.repository.object.composition.ClassFactory;
import org.openrdf.repository.object.managers.PropertyMapper;

public class BehaviourProviderService {
	private static final String SERVICES = "META-INF/services/"
			+ BehaviourProvider.class.getName();
	private ClassFactory cl;

	public BehaviourProviderService(ClassFactory cl) {
		this.cl = cl;
	}

	public Collection<BehaviourFactory> findImplementations(PropertyMapper mapper,
			Collection<Class<?>> classes, Set<Class<?>> bases)
			throws IOException {
		List<BehaviourFactory> implementations = new ArrayList<BehaviourFactory>();
		Enumeration<URL> resources = cl.getResources(SERVICES);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			Properties properties = new OrderedProperties(url.openStream());
			for (Object key : properties.keySet()) {
				BehaviourProvider bf;
				try {
					bf = (BehaviourProvider) cl.newInstance((String) key);
					bf.setClassDefiner(cl);
					bf.setPropertyMapper(mapper);
					bf.setBaseClasses(bases);
				} catch (Exception e) {
					throw new ServiceConfigurationError(e.toString(), e);
				}
				implementations.addAll(bf.getBehaviourFactories(classes));
			}
		}
		return implementations;
	}

}
