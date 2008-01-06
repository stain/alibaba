package org.openrdf.alibaba.pov;

import org.openrdf.alibaba.core.Property;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;

public interface AltDisplayOrBagDisplayOrNestedPropertyDisplayOrSeqDisplay  {


	/** The RDF properties shown in this display. */
	@rdf("http://www.openrdf.org/rdf/2007/09/point-of-view#properties")
	public abstract Container<Property> getPovProperties();

	/** The RDF properties shown in this display. */
	public abstract void setPovProperties(Container<Property> value);

}
