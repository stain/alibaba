package org.openrdf.alibaba.pov.support;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.CollectionDisplay;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.annotations.rdf;

@rdf(POV.NS + "CollectionDisplay")
public class CollectionDisplaySupport extends DisplaySupport implements DisplayBehaviour {

	public CollectionDisplaySupport(CollectionDisplay display) {
		super(display);
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		assert resource instanceof Collection : resource;
		return (Collection<?>) resource;
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		assert resource instanceof Collection : resource;
		if (!getValuesOf(resource).equals(values)) {
			if (resource instanceof List) {
				List<Object> list = (List<Object>) resource;
				int size = list.size();
				if (list.size() > values.size()) {
					list.retainAll(values);
					size = list.size();
				}
				Iterator<?> iter = values.iterator();
				for (int i = 0; iter.hasNext(); i++) {
					Object next = iter.next();
					if (i >= size) {
						list.add(next);
					} else if (!equals(next, list.get(i))) {
						list.set(i, next);
					}
				}
			} else if (resource instanceof Set) {
				Set<Object> set = (Set<Object>) resource;
				set.retainAll(values);
				set.addAll(values);
			} else {
				Collection<Object> coll = (Collection<Object>) resource;
				coll.clear();
				coll.addAll(values);
			}
		}
	}

	private boolean equals(Object next, Object object) {
		return next == object || next != null && next.equals(object);
	}
}
