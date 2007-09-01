package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Layout;
import org.openrdf.alibaba.repositories.RepositoryBehaviour;

public interface RepresentationRepositoryBehaviour extends
		RepositoryBehaviour<Representation> {
	public abstract Representation findRepresentation(Intent intention,
			Layout layout);
}
