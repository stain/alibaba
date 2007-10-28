package org.openrdf.alibaba.servlet;

import javax.xml.namespace.QName;

import org.openrdf.alibaba.decor.PresentationService;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.concepts.rdfs.Class;

public interface PresentationManager {
	public boolean isOpen();

	public void close();

	public PresentationService findPresentationService();

	public Intent findIntent(QName intent);

	public Class findClass(QName type);
}
