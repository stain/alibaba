package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.core.RepositoryBehaviour;
import org.openrdf.alibaba.formats.Layout;
import org.openrdf.alibaba.pov.Intent;

/**
 * {@link Representation} lookup method.
 * 
 * @author James Leigh
 *
 */
public interface RepresentationRepositoryBehaviour extends
		RepositoryBehaviour<Representation> {
	public abstract Representation findRepresentation(Intent intention,
			Layout layout);
}
