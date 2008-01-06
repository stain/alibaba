package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.core.RepositoryBehaviour;
import org.openrdf.alibaba.pov.Intent;

/**
 * Methods to lookup {@link Presentation}s.
 * 
 * @author James Leigh
 *
 */
public interface PresentationRepositoryBehaviour extends RepositoryBehaviour<Presentation> {
	public abstract Presentation findPresentation(Intent intent, String... accept);
}
