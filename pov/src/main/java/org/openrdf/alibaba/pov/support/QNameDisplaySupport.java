package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.QNameDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;

/**
 * Used for displaying a qualified name.
 * 
 * @author James Leigh
 *
 */
@rdf(POV.NS + "QNameDisplay")
public class QNameDisplaySupport extends DisplaySupport implements DisplayBehaviour {

	public QNameDisplaySupport(QNameDisplay display) {
		super(display);
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		if (resource == null)
			return Collections.EMPTY_SET;
		assert resource instanceof Entity : resource;
		QName name = ((Entity) resource).getQName();
		return new ArrayList<Object>(Arrays.asList(name));
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Entity e = (Entity) resource;
			assert values.size() == 1 : values;
			for (Object value : values) {
				assert value instanceof QName : value;
				e.getElmoManager().rename(e, (QName) value);
			}
		}
	}
}
