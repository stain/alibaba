package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.NestedPropertyDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

/**
 * The method getValuesOf for properties "bean" followed by "value"
 * will return the values <code>resource.getBean().getValue()</code>.
 * 
 * @author James Leigh
 * 
 */
@rdf(POV.NS + "NestedPropertyDisplay")
public class NestedPropertyDisplaySupport extends DisplaySupport implements
		DisplayBehaviour {
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
			value = prop.getValueOf(value);
		}
		return value;
	}

	private void setPropertyValue(Object resource, Collection<?> values)
			throws AlibabaException {
		Object obj = resource;
		Iterator<Property> iter = display.getPovProperties().iterator();
		while (iter.hasNext()) {
			Property prop = iter.next();
			if (iter.hasNext()) {
				obj = prop.getValueOf(obj);
			} else {
				prop.setValueOf(obj, values);
			}
		}
	}
}
