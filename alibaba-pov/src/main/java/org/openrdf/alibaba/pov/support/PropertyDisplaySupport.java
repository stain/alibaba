package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.PropertyDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

/**
 * Used for displaying a property value.
 * 
 * @author James Leigh
 *
 */
@rdf(POV.NS + "PropertyDisplay")
public class PropertyDisplaySupport extends DisplaySupport implements DisplayBehaviour {
	private PropertyDisplay display;

	public PropertyDisplaySupport(PropertyDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Property prop = display.getPovProperty();
		Object value = prop.getValueOf(resource);
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
			prop.setValueOf(resource, values);
		}
	}
}
