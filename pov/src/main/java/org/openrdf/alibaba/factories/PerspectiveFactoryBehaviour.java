package org.openrdf.alibaba.factories;

import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.Perspective;
import org.openrdf.elmo.Entity;

/**
 * Method that is used to create {@link Perspective}s.
 * 
 * @author James Leigh
 * 
 */
public interface PerspectiveFactoryBehaviour {
	public abstract Perspective createPerspectiveFor(Intent intention,
			Entity entity);
}