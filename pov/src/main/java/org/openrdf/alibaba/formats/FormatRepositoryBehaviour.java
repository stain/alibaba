package org.openrdf.alibaba.formats;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.RepositoryBehaviour;

/**
 * Methods used to lookup Formats.
 * 
 * @author James Leigh
 * 
 */
public interface FormatRepositoryBehaviour extends RepositoryBehaviour<Format> {
	public abstract Format findFormat(QName qname);
}
