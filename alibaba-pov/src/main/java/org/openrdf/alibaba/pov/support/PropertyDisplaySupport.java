package org.openrdf.alibaba.pov.support;

import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.PropertyDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "PropertyDisplay")
public class PropertyDisplaySupport extends PropertyOrCollectionDisplaySupport implements DisplayBehaviour {
	private PropertyDisplay display;

	public PropertyDisplaySupport(PropertyDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Property prop = display.getPovProperty();
		Object value = getPropertyValue(resource, prop.getQName());
		if (value instanceof Collection)
			return (Collection) value;
		if (value == null)
			return Collections.EMPTY_SET;
		return Collections.singleton(value);
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Property prop = display.getPovProperty();
			QName name = prop.getQName();
			setPropertyValue(resource, name, values);
		}
	}
}
