package org.openrdf.alibaba.factories;

import org.openrdf.alibaba.formats.Format;
import org.openrdf.alibaba.formats.Style;
import org.openrdf.alibaba.pov.Display;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdf.Property;

public interface DisplayFactoryBehaviour {

	public abstract Display createDisplay();

	public abstract Display createDisplay(Format format);

	public abstract Display createDisplay(Format format, Style style);

	public abstract Display createBindingDisplay(String name);

	public abstract Display createBindingDisplay(String name,
			Format format);

	public abstract Display createPropertyDisplay(DatatypeProperty property);

	public abstract Display createPropertyDisplay(ObjectProperty property);

	public abstract Display createPropertyDisplay(Property property,
			Format format);

}