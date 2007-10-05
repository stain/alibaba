package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.pov.Display;

public interface RepresentationBehaviour {
	public abstract Decoration findDecorationFor(Display display);
}
