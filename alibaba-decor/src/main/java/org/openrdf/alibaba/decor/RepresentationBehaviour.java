package org.openrdf.alibaba.decor;

import org.openrdf.alibaba.pov.Display;

/**
 * Method to choose a {@link Decoration} from a {@link Display}.
 * 
 * @author James Leigh
 *
 */
public interface RepresentationBehaviour {
	public abstract Decoration findDecorationFor(Display display);
}
