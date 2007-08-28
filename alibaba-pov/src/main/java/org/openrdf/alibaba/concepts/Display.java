package org.openrdf.alibaba.concepts;

import org.openrdf.alibaba.behaviours.DisplayBehaviour;
import org.openrdf.concepts.owl.Thing;
import org.openrdf.concepts.rdf.Property;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;

/** Description of how a value should be shown. */
@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#Display")
public interface Display extends Thing, DisplayOrExpression, DisplayBehaviour {


	/** The RDF propertyies shown in this display. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#properties")
	public abstract Container<Property> getPovProperties();

	/** The RDF propertyies shown in this display. */
	public abstract void setPovProperties(Container<Property> value);


	/** The width and height that should be used to display the values. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#size")
	public abstract Size getPovSize();

	/** The width and height that should be used to display the values. */
	public abstract void setPovSize(Size value);


	/** The style that should be used for this display. */
	@rdf("http://www.openrdf.org/rdf/2007/08/point-of-view#style")
	public abstract Style getPovStyle();

	/** The style that should be used for this display. */
	public abstract void setPovStyle(Style value);

}
