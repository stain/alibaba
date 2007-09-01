package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.pov.PerspectiveOrSearchPattern;
import org.openrdf.elmo.Entity;

public interface PresentationBehaviour {
	public abstract PerspectiveOrSearchPattern findPerspectiveOrSearchPattern(Intent intent, Entity target);
}
