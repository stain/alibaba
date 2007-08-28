package org.openrdf.alibaba.repositories;

import org.openrdf.alibaba.concepts.Presentation;

public interface PresentationRepositoryBehaviour extends RepositoryBehaviour<Presentation> {
	public abstract Presentation findPresentation(String... accept);
}
