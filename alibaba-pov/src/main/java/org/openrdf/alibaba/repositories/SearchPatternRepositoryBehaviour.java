package org.openrdf.alibaba.repositories;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.SearchPattern;
import org.openrdf.concepts.rdfs.Class;

public interface SearchPatternRepositoryBehaviour extends RepositoryBehaviour<SearchPattern> {
	public abstract SearchPattern findSearchPattern(QName qname);

	public abstract SearchPattern findSearchPattern(Intent intention, Class type);
}
