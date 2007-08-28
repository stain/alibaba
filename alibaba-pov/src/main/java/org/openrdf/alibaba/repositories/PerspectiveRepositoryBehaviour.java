package org.openrdf.alibaba.repositories;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.Perspective;
import org.openrdf.elmo.Entity;

public interface PerspectiveRepositoryBehaviour extends RepositoryBehaviour<Perspective> {
	public abstract Perspective findPerspective(QName qname);

	public abstract Perspective findPerspective(Intent intention, Entity target);
}
