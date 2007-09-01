package org.openrdf.alibaba.pov;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.formats.Format;

public interface FormatRepositoryBehaviour extends RepositoryBehaviour<Format> {
	public abstract Format findFormat(QName qname);

	public abstract Format findFormatFor(Object value);
}
