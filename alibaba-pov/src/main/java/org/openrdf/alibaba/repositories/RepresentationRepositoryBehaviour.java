package org.openrdf.alibaba.repositories;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Layout;
import org.openrdf.alibaba.concepts.Representation;

public interface RepresentationRepositoryBehaviour extends
		RepositoryBehaviour<Representation> {
	public abstract Representation findRepresentation(Intent intention,
			Layout layout);
}
