package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.SeqDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "SeqDisplay")
public class SeqDisplaySupport extends PropertyOrCollectionDisplaySupport implements DisplayBehaviour {
	private SeqDisplay display;

	public SeqDisplaySupport(SeqDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		List<Object> list = new ArrayList<Object>();
		for (Property prop : display.getPovProperties()) {
			Object value = getPropertyValue(resource, prop.getQName());
			if (value instanceof Collection) {
				Collection<?> set = (Collection<?>) value;
				int size = set.size();
				if (size == 0) {
					list.add(null);
				} else if (size == 1) {
					list.addAll(set);
				} else {
					list.add(set.toArray()[0]);
				}
			} else {
				list.add(value);
			}
		}
		return Collections.singleton(list.toArray());
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Container<Property> props = display.getPovProperties();
				Iterator<?> iter = values.iterator();
				for (Property prop : props) {
					if (iter.hasNext()) {
						setPropertyValue(resource, prop.getQName(), iter.next());
					}
				}
		}
	}
}
