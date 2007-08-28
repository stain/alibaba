package org.openrdf.alibaba.factories;

import org.openrdf.alibaba.concepts.Display;
import org.openrdf.alibaba.concepts.Format;
import org.openrdf.alibaba.concepts.LiteralDisplay;
import org.openrdf.alibaba.concepts.Style;
import org.openrdf.concepts.owl.DatatypeProperty;
import org.openrdf.concepts.owl.ObjectProperty;
import org.openrdf.concepts.rdf.Property;

public interface DisplayFactoryBehaviour {

	public abstract Display createDisplay();

	public abstract Display createDisplay(Format format);

	public abstract Display createDisplay(Format format, Style style);

	public abstract LiteralDisplay createBindingDisplay(String name);

	public abstract LiteralDisplay createBindingDisplay(String name,
			Format format);

	public abstract Display createPropertyDisplay(DatatypeProperty property);

	public abstract Display createPropertyDisplay(ObjectProperty property);

	public abstract Display createPropertyDisplay(Property property,
			Format format);

}