package org.openrdf.alibaba.pov.support;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.BagDisplay;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

/**
 * Combines property values into a single collection.
 * 
 * @author James Leigh
 * 
 */
@rdf(POV.NS + "BagDisplay")
public class BagDisplaySupport extends DisplaySupport implements
		DisplayBehaviour {
	private BagDisplay display;

	public BagDisplaySupport(BagDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Collection<Object> values = new LinkedHashSet<Object>();
		for (Property prop : display.getPovProperties()) {
			Object value = prop.getValueOf(resource);
			if (value instanceof Collection) {
				values.addAll((Collection<?>) value);
			} else if (value != null) {
				values.add(value);
			}
		}
		return values;
	}
}
