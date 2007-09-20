package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.core.RepositoryBehaviour;

public interface DisplayRepositoryBehaviour extends RepositoryBehaviour<Display> {
	public abstract Display findDisplay(QName qname);
}
