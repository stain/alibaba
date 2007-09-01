package org.openrdf.alibaba.factories;

import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.elmo.Entity;

public interface PerspectiveFactoryBehaviour {
	public abstract Perspective createPerspectiveFor(Intent intention, Entity entity);
}