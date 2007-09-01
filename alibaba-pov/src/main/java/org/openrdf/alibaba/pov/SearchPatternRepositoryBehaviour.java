package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.concepts.rdfs.Class;

public interface SearchPatternRepositoryBehaviour extends RepositoryBehaviour<SearchPattern> {
	public abstract SearchPattern findSearchPattern(QName qname);

	public abstract SearchPattern findSearchPattern(Intent intention, Class type);
}
