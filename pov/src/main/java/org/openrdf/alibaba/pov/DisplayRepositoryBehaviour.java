package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.RepositoryBehaviour;

/**
 * Method to find a display by name.
 * 
 * @author James Leigh
 * 
 */
public interface DisplayRepositoryBehaviour extends
		RepositoryBehaviour<Display> {
	public abstract Display findDisplay(QName qname);
}
