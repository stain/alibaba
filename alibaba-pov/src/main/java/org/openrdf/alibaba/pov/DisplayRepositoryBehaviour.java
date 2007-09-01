package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.concepts.rdf.Property;

public interface DisplayRepositoryBehaviour extends RepositoryBehaviour<Display> {
	public abstract Display findDisplay(QName qname);

	public abstract Display findDisplayFor(Property property);
}
