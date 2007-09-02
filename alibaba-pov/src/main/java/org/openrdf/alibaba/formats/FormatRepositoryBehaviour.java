package org.openrdf.alibaba.formats;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.pov.RepositoryBehaviour;

public interface FormatRepositoryBehaviour extends RepositoryBehaviour<Format> {
	public abstract Format findFormat(QName qname);

	public abstract Format findFormatFor(Object value);
}
