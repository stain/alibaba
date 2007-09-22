package org.openrdf.alibaba.pov;

import org.openrdf.concepts.rdf.Property;
import org.openrdf.elmo.annotations.rdf;

/** Description of how a property value should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#PropertyDisplay")
public interface PropertyDisplay extends Display {


	/** The RDF property shown in this display. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#property")
	public abstract Property getPovProperty();

	/** The RDF property shown in this display. */
	public abstract void setPovProperty(Property value);

}
