package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.concepts.Intent;
import org.openrdf.alibaba.concepts.PerspectiveOrSearchPattern;
import org.openrdf.elmo.Entity;

public interface PresentationBehaviour {
	public abstract PerspectiveOrSearchPattern findPerspectiveOrSearchPattern(Intent intent, Entity target);
}
