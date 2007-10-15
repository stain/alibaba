package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.PropertyDisplay;
import org.openrdf.alibaba.pov.helpers.PropertyValuesHelper;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "PropertyDisplay")
public class PropertyDisplaySupport extends DisplaySupport implements DisplayBehaviour {
	private static PropertyValuesHelper helper = new PropertyValuesHelper();

	private PropertyDisplay display;

	public PropertyDisplaySupport(PropertyDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Property prop = display.getPovProperty();
		Object value = helper.getPropertyValue(resource, prop.getQName());
		if (value instanceof Collection)
			return (Collection) value;
		if (value == null)
			return new ArrayList<Object>();
		return new ArrayList<Object>(Arrays.asList(value));
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Property prop = display.getPovProperty();
			QName name = prop.getQName();
			helper.setPropertyValue(resource, name, values);
		}
	}
}
