package org.openrdf.alibaba.servlet;

import java.util.Locale;

public interface PresentationManagerFactory {
	public boolean isOpen();

	public void close();

	public PresentationManager createManager(Locale locale);
}
