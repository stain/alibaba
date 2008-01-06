package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.RepositoryBehaviour;
import org.openrdf.elmo.Entity;

/**
 * Lookup methods for {@link Perspective}s.
 * 
 * @author James Leigh
 *
 */
public interface PerspectiveRepositoryBehaviour extends RepositoryBehaviour<Perspective> {
	public abstract Perspective findPerspective(QName qname);

	public abstract Perspective findPerspective(Intent intention, Entity target);
}
