package org.openrdf.alibaba.repositories;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.concepts.Format;

public interface FormatRepositoryBehaviour extends RepositoryBehaviour<Format> {
	public abstract Format findFormat(QName qname);

	public abstract Format findFormatFor(Object value);
}
