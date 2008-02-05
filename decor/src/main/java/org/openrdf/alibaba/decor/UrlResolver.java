package org.openrdf.alibaba.decor;

import org.openrdf.elmo.Entity;

/**
 * Creates URLs for given entities.
 * 
 * @author James Leigh
 *
 */
public interface UrlResolver {
	public String resolve(Entity entity);

	public String resolve(Entity entity, String mimeType);
}
