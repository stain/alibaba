package org.openrdf.alibaba.pov.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.AltDisplay;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;

/** Searches alternative property values for a non-null and non-empty value. */
@rdf(POV.NS + "AltDisplay")
public class AltDisplaySupport extends DisplaySupport implements DisplayBehaviour {
	private AltDisplay display;

	public AltDisplaySupport(AltDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		Object value = null;
		Container<Property> props = display.getPovProperties();
		for (Property prop : props) {
			value = prop.getValueOf(resource);
			boolean empty = value instanceof Collection
					&& ((Collection) value).isEmpty();
			if (value != null && !empty)
				break;
		}
		if (props instanceof Closeable) {
			try {
				((Closeable) props).close();
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
		if (value instanceof Collection) {
			return (Collection) value;
		}
		if (value == null)
			return new ArrayList<Object>();
		return new ArrayList<Object>(Arrays.asList(value));
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Container<Property> props = display.getPovProperties();
			Iterator<?> iter = values.iterator();
			for (Property prop : props) {
				if (iter.hasNext()) {
					prop.setValueOf(resource, iter.next());
				}
			}
		}
	}
}
