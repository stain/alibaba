package org.openrdf.repository.object.composition;

import java.util.Collection;
import java.util.Set;

import org.openrdf.repository.object.managers.PropertyMapper;

public interface BehaviourProvider {

	void setClassDefiner(ClassFactory definer);

	void setBaseClasses(Set<Class<?>> bases);

	void setPropertyMapper(PropertyMapper mapper);

	Collection<BehaviourFactory> findImplementations(Collection<Class<?>> classes);

}