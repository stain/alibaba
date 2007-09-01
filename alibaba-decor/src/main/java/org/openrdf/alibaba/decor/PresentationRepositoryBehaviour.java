package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.pov.RepositoryBehaviour;


public interface PresentationRepositoryBehaviour extends RepositoryBehaviour<Presentation> {
	public abstract Presentation findPresentation(String... accept);
}
