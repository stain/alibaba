package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.NestedPropertyDisplay;
import org.openrdf.alibaba.pov.helpers.PropertyValuesHelper;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "NestedPropertyDisplay")
public class NestedPropertyDisplaySupport extends DisplaySupport implements
		DisplayBehaviour {
	private static PropertyValuesHelper helper = new PropertyValuesHelper();

	private NestedPropertyDisplay display;

	public NestedPropertyDisplaySupport(NestedPropertyDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Object value = getPropertyValue(resource);
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
			setPropertyValue(resource, values);
		}
	
	}

	private Object getPropertyValue(Object resource) throws AlibabaException {
		Object value = resource;
		for (Property prop : display.getPovProperties()) {
			value = helper.getPropertyValue(value, prop.getQName());
		}
		return value;
	}

	private void setPropertyValue(Object resource, Collection<?> values) throws AlibabaException {
		Object obj = resource;
		Iterator<Property> iter = display.getPovProperties().iterator();
		while (iter.hasNext()) {
			Property prop = iter.next();
			if (iter.hasNext()) {
				obj = helper.getPropertyValue(obj, prop.getQName());
			} else {
				helper.setPropertyValue(obj, prop.getQName(), values);
			}
		}
	}
}
