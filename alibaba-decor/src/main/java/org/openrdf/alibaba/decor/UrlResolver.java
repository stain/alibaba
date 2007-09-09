package org.openrdf.alibaba.decor;

import org.openrdf.elmo.Entity;

public interface UrlResolver {
	public String resolve(Entity entity);

	public String resolve(Entity entity, String mimeType);
}
