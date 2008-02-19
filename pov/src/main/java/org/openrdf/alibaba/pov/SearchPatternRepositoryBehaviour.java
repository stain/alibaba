package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.RepositoryBehaviour;
import org.openrdf.concepts.rdfs.Class;

/**
 * Lookup methods for {@link SearchPattern}s.
 * 
 * @author James Leigh
 *
 */
public interface SearchPatternRepositoryBehaviour extends RepositoryBehaviour<SearchPattern> {
	public abstract SearchPattern findSearchPattern(QName qname);

	public abstract SearchPattern findSearchPattern(Intent intention, Class type);
}